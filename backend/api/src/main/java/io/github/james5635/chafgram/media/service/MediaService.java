package io.github.james5635.chafgram.media.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.common.service.AuditService;
import io.github.james5635.chafgram.identity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Map;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.media.utils.BlurhashEncoder;
import io.github.james5635.chafgram.media.utils.MediaStorage;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.MediaStatus;

/** Media asset lifecycle. Images complete instantly; videos queue for async transcode. */
@Service
public class MediaService {

	private static final Logger log = LoggerFactory.getLogger(MediaService.class);

	private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
	private static final long MAX_VIDEO_BYTES = 4L * 1024 * 1024 * 1024;

	private final MediaStorage storage;
	private final MediaAssetRepository mediaAssets;
	private final ApplicationEventPublisher eventPublisher;
	private final AuditService audit;

	public MediaService(MediaStorage storage, MediaAssetRepository mediaAssets,
			ApplicationEventPublisher eventPublisher, AuditService audit) {
		this.storage = storage;
		this.mediaAssets = mediaAssets;
		this.eventPublisher = eventPublisher;
		this.audit = audit;
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

		if (kind == MediaKind.VIDEO) {
			// The listener starts the video pipeline after this transaction commits.
			eventPublisher.publishEvent(saved.getId());
		} else {
			populateImageMetadata(saved);
		}
		return saved;
	}

	/**
	 * Best-effort image metadata (FR-5.2): dimensions and blurhash, read back
	 * from the stored file. Images complete instantly, so the upload itself
	 * carries the required metadata — there is no async image processor.
	 * Formats ImageIO cannot read (e.g. WebP) simply leave the fields null.
	 * <p>
	 * Shared by {@code POST /api/v1/media} and the inline message upload, so
	 * both image paths expose the same metadata.
	 */
	public void populateImageMetadata(MediaAsset asset) {
		try {
			BufferedImage image = ImageIO.read(storage.resolvePath(asset.getStorageKey()).toFile());
			if (image != null) {
				asset.setWidth(image.getWidth());
				asset.setHeight(image.getHeight());
				asset.setBlurhash(BlurhashEncoder.encode(image, 4, 3));
				mediaAssets.save(asset);
			}
		}
		catch (Exception e) {
			log.warn("Image metadata not populated for media {}: {}", asset.getId(), e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public MediaAsset get(Long id) {
		return mediaAssets.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
	}

	/**
	 * Admin moderation delete (BR-5.3): tombstones the asset so it is no
	 * longer served anywhere, and records the action in the audit log
	 * (BR-9.2).
	 *
	 * <p>The bytes stay on disk — physical purge is a retention job
	 * (BR-5.4, still a proposal). Re-deleting an already deleted asset is a
	 * no-op.</p>
	 */
	@Transactional
	public void deleteForModeration(User actor, Long mediaId) {
		MediaAsset asset = mediaAssets.findById(mediaId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
		if (asset.getStatus() == MediaStatus.DELETED) {
			return;
		}
		asset.setStatus(MediaStatus.DELETED);
		mediaAssets.save(asset);
		audit.log(actor.getId(), "media.delete", "media", asset.getId(),
				Map.of("ownerUserId", asset.getOwnerUserId(),
						"kind", asset.getKind().name().toLowerCase()));
	}

	public MediaStorage.MediaStream read(MediaAsset asset) {
		return storage.read(asset.getStorageKey());
	}

	/** Reads an arbitrary stored blob (the video thumbnail uses its own key). */
	public MediaStorage.MediaStream read(String storageKey) {
		return storage.read(storageKey);
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
