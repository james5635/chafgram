package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Shared checks for media a member wants to attach to something they own
 * (avatar, workspace logo, post/message attachments): the asset must exist,
 * be owned by the actor, be {@code ready}, and match the allowed kinds.
 */
@Service
public class MediaValidation {

	private final MediaAssetRepository mediaAssets;

	public MediaValidation(MediaAssetRepository mediaAssets) {
		this.mediaAssets = mediaAssets;
	}

	/**
	 * Validates {@code mediaIds} for attachment by {@code actor}, preserving order.
	 * Empty/null input yields an empty list. Throws 400 (too many / wrong kind /
	 * not ready), 404 (unknown id) or 403 (owned by another member).
	 */
	public List<MediaAsset> validateAttachable(User actor, List<Long> mediaIds, int max,
			MediaKind... allowedKinds) {
		if (mediaIds == null || mediaIds.isEmpty()) {
			return List.of();
		}
		if (mediaIds.size() > max) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"At most " + max + " media item(s) can be attached");
		}
		return mediaIds.stream()
				.map(id -> validateOne(actor, id, allowedKinds))
				.toList();
	}

	private MediaAsset validateOne(User actor, Long id, MediaKind... allowedKinds) {
		MediaAsset asset = mediaAssets.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
		if (!asset.getOwnerUserId().equals(actor.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Media belongs to another member");
		}
		if (asset.getStatus() != MediaStatus.READY) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Media is not ready yet");
		}
		if (allowedKinds.length > 0) {
			for (MediaKind kind : allowedKinds) {
				if (asset.getKind() == kind) {
					return asset;
				}
			}
			throw new ApiException(HttpStatus.BAD_REQUEST, "Media has the wrong kind");
		}
		return asset;
	}
}
