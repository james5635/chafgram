package io.github.james5635.chafgram.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Stores media under a configurable root (default ./media-data) with opaque keys. */
@Service
public class LocalMediaStorage implements MediaStorage {

	private final Path root;

	public LocalMediaStorage(@Value("${chafgram.media.storage-root:./media-data}") String root) {
		this.root = Path.of(root).toAbsolutePath().normalize();
	}

	@Override
	public String store(MultipartFile file, String extension) {
		String key = "uploads/" + UUID.randomUUID() + extension;
		Path target = root.resolve(key);
		try {
			Files.createDirectories(target.getParent());
			file.transferTo(target);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to store media", e);
		}
		return key;
	}

	@Override
	public MediaStream read(String storageKey) {
		Path path = root.resolve(storageKey).normalize();
		if (!path.startsWith(root) || !Files.exists(path)) {
			throw new IllegalArgumentException("Unknown storage key");
		}
		try {
			return new MediaStream(Files.newInputStream(path), probeContentType(path), Files.size(path));
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read media", e);
		}
	}

	private String probeContentType(Path path) {
		try {
			return Files.probeContentType(path) != null ? Files.probeContentType(path) : "application/octet-stream";
		}
		catch (IOException e) {
			return "application/octet-stream";
		}
	}
}
