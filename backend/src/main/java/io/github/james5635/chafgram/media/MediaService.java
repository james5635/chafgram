package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Media asset lifecycle. Images complete instantly; videos queue for async transcode. */
@Service
public class MediaService {

	private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
	private static final long MAX_VIDEO_BYTES = 4L * 1024 * 1024 * 1024;

	private final MediaStorage storage;
	private final MediaAssetRepository mediaAssets;
	private final ApplicationEventPublisher eventPublisher;

	public MediaService(MediaStorage storage, MediaAssetRepository mediaAssets, ApplicationEventPublisher eventPublisher) {
		this.storage = storage;
		this.mediaAssets = mediaAssets;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public MediaAsset upload(User actor, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "A file is required");
		}
		String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
		MediaKind kind = kindOf(mime, file);
		long size = file.getSize();
		if (kind == MediaKind.IMAGE && size > MAX_IMAGE_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Images are limited to 20 MB");
		}
		if (kind == MediaKind.VIDEO && size > MAX_VIDEO_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Videos are limited to 4 GB");
		}

		String extension = extensionOf(file.getOriginalFilename());
		String key = storage.store(file, extension);

		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(actor.getId());
		asset.setKind(kind);
		asset.setStatus(kind == MediaKind.IMAGE ? MediaStatus.READY : MediaStatus.PROCESSING);
		asset.setStorageKey(key);
		asset.setMimeType(mime.isEmpty() ? "application/octet-stream" : mime);
		asset.setSizeBytes(size);
		MediaAsset saved = mediaAssets.save(asset);

		// The listener starts the video pipeline after this transaction commits.
		if (kind == MediaKind.VIDEO) {
			eventPublisher.publishEvent(saved.getId());
		}
		return saved;
	}

	@Transactional(readOnly = true)
	public MediaAsset get(Long id) {
		return mediaAssets.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
	}

	public MediaStorage.MediaStream read(MediaAsset asset) {
		return storage.read(asset.getStorageKey());
	}

	private MediaKind kindOf(String mime, MultipartFile file) {
		if (mime.startsWith("image/")) {
			return MediaKind.IMAGE;
		}
		if (mime.startsWith("video/")) {
			return MediaKind.VIDEO;
		}
		throw new ApiException(HttpStatus.BAD_REQUEST, "Only image and video uploads are supported");
	}

	private String extensionOf(String filename) {
		if (filename == null) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot).toLowerCase();
	}
}
