package io.github.james5635.chafgram.media;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

/** Storage backend for media blobs — local disk today, S3-compatible later. */
public interface MediaStorage {

	String store(MultipartFile file, String extension);

	void storeProcessed(Path source, String storageKey);

	Path resolvePath(String storageKey);

	MediaStream read(String storageKey);

	record MediaStream(InputStream stream, String contentType, long sizeBytes) {
	}
}
