package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.media.utils.MediaStorage;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** MediaStorage for hermetic tests: bytes live in memory, no files are written. */
public class InMemoryMediaStorage implements MediaStorage {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    /** Test helper: put raw bytes at a storage key. */
    public void put(String storageKey, byte[] bytes) {
        objects.put(storageKey, bytes.clone());
    }

    @Override
    public String store(MultipartFile file, String extension) {
        String key = "uploads/" + UUID.randomUUID() + extension;
        try {
            put(key, file.getBytes());
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to store media", e);
        }
        return key;
    }

    @Override
    public void storeProcessed(Path source, String storageKey) {
        try {
            put(storageKey, Files.readAllBytes(source));
            Files.deleteIfExists(source);
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to store processed media", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        objects.remove(storageKey);
    }

    @Override
    public Path resolvePath(String storageKey) {
        byte[] bytes = objects.get(storageKey);
        if (bytes == null) {
            throw new IllegalArgumentException("Unknown storage key: " + storageKey);
        }
        try {
            Path temp = Files.createTempFile("chafgram-test-", "-" + storageKey.replace('/', '_'));
            Files.write(temp, bytes);
            return temp;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to stage media " + storageKey, e);
        }
    }

    @Override
    public MediaStream read(String storageKey) {
        byte[] bytes = objects.get(storageKey);
        if (bytes == null) {
            throw new IllegalArgumentException("Unknown storage key: " + storageKey);
        }
        String contentType = contentType(storageKey);
        return new MediaStream(new ByteArrayInputStream(bytes), contentType, bytes.length);
    }

    private String contentType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        return "application/octet-stream";
    }
}
