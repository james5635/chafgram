package io.github.james5635.chafgram.media.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/** Storage backend for media blobs — local disk today, S3-compatible later. */
public interface MediaStorage {

	String store(MultipartFile file, String extension);

	void storeProcessed(Path source, String storageKey);

	/** Removes a blob — used to drop a source file the pipeline replaced. */
	void delete(String storageKey);

	Path resolvePath(String storageKey);

	MediaStream read(String storageKey);

	/** Storage-agnostic existence check. */
	default boolean exists(String storageKey) {
		try (MediaStream stream = read(storageKey)) {
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	record MediaStream(InputStream stream, String contentType, long sizeBytes) implements AutoCloseable {
		@Override
		public void close() throws IOException {
			stream.close();
		}
	}
}
