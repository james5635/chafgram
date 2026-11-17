package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.media.dto.MediaAssetDto;
import io.github.james5635.chafgram.social.CurrentMember;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/** Media metadata on GraphQL; bytes stay on REST (`GET /api/v1/media/{id}/bytes`). */
@Controller
@PreAuthorize("isFullyAuthenticated()")
public class MediaQueryResolver {

	private final MediaService media;
	private final CurrentMember current;

	public MediaQueryResolver(MediaService media, CurrentMember current) {
		this.media = media;
		this.current = current;
	}

	@QueryMapping
	public MediaAssetDto mediaAsset(@Argument Long id) {
		current.require();
		MediaAsset asset = media.get(id);
		if (asset.getStatus() == MediaStatus.DELETED) {
			// Admin-deleted media (BR-5.3) is not visible to anyone.
			throw new ApiException(HttpStatus.NOT_FOUND, "Media not found");
		}
		return new MediaAssetDto(
				asset.getId(),
				asset.getKind().name().toLowerCase(),
				asset.getStatus().name().toLowerCase(),
				asset.getOwnerUserId(),
				asset.getMimeType(),
				asset.getWidth(),
				asset.getHeight(),
				asset.getDurationMs(),
				asset.getBlurhash(),
				asset.getSizeBytes() == null ? null : String.valueOf(asset.getSizeBytes()),
				asset.getCreatedAt());
	}
}
