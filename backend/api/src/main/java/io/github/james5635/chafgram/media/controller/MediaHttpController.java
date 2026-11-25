package io.github.james5635.chafgram.media.controller;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import io.github.james5635.chafgram.media.service.MediaAccessService;
import io.github.james5635.chafgram.media.service.MediaService;
import io.github.james5635.chafgram.media.MediaAsset;

/**
 * REST-only: multipart upload and byte streaming. Both are fundamentally
 * binary HTTP operations — not expressible as GraphQL. Media metadata lives
 * on the GraphQL surface (`mediaAsset`).
 */
@RestController
public class MediaHttpController {

	private final MediaService media;
	private final MemberAccess access;
	private final MediaAccessService mediaAccess;

	public MediaHttpController(MediaService media, MemberAccess access,
			MediaAccessService mediaAccess) {
		this.media = media;
		this.access = access;
		this.mediaAccess = mediaAccess;
	}

	@PostMapping(value = "/api/v1/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
		User actor = access.requireWriteable(currentUsername());
		MediaAsset asset = media.upload(actor, file);
		return Map.of(
				"id", asset.getId(),
				"kind", asset.getKind().name().toLowerCase(),
				"status", asset.getStatus().name().toLowerCase(),
				"storageKey", asset.getStorageKey(),
				"sizeBytes", asset.getSizeBytes());
	}

	/**
	 * Authorized byte streaming. {@code ?variant=thumbnail} serves the
	 * generated video thumbnail (image/jpeg) instead of the asset itself —
	 * the same authorization decision applies to both.
	 */
	@GetMapping("/api/v1/media/{id}/bytes")
	public ResponseEntity<byte[]> bytes(@PathVariable Long id,
			@RequestParam(required = false) String variant) {
		// Reads stay open for suspended members (FR-1.4: read but not write)
		User actor = access.currentMember(currentUsername()); // members only, never anonymous (BR-5.2)
		MediaAsset asset = media.get(id);
		mediaAccess.assertCanView(actor, asset); // authorization: blocks/tombstones → 404
		boolean thumbnail = "thumbnail".equals(variant) && asset.getThumbnailKey() != null;
		String key = thumbnail ? asset.getThumbnailKey() : asset.getStorageKey();
		String mimeType = thumbnail ? "image/jpeg" : asset.getMimeType();
		try (InputStream in = media.read(key).stream()) {
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(mimeType))
					.body(in.readAllBytes());
		}
		catch (IOException e) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to stream media");
		}
	}

	private String currentUsername() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}
}
