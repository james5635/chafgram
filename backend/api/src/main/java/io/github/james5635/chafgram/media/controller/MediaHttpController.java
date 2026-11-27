package io.github.james5635.chafgram.media.controller;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import io.github.james5635.chafgram.media.service.MediaUrlService;
import io.github.james5635.chafgram.media.MediaAsset;

/**
 * REST-only: multipart upload, signed-URL issuance, and byte streaming. All
 * three are fundamentally binary/HTTP operations — not expressible as GraphQL.
 * Media metadata lives on the GraphQL surface ({@code mediaAsset}).
 */
@RestController
public class MediaHttpController {

	private final MediaService media;
	private final MemberAccess access;
	private final MediaAccessService mediaAccess;
	private final MediaUrlService mediaUrls;
	private final UserRepository users;

	public MediaHttpController(MediaService media, MemberAccess access,
			MediaAccessService mediaAccess, MediaUrlService mediaUrls, UserRepository users) {
		this.media = media;
		this.access = access;
		this.mediaAccess = mediaAccess;
		this.mediaUrls = mediaUrls;
		this.users = users;
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
	 * Bearer-protected, on-demand re-signing for clients that need a fresh URL
	 * (for example a long-open feed whose media returned an error). The viewer
	 * must still be allowed to read the asset — this endpoint never widens
	 * {@link MediaAccessService}'s decision.
	 */
	@GetMapping("/api/v1/media/{id}/signed-url")
	public Map<String, Object> signedUrl(@PathVariable Long id) {
		User actor = access.currentMember(currentUsername()); // members only
		MediaAsset asset = media.get(id);
		mediaAccess.assertCanView(actor, asset);
		MediaUrlService.SignedUrl signed = mediaUrls.issue(asset.getId(), actor.getId());
		return Map.of(
				"url", signed.url(),
				"expiresAt", signed.expiresAt().toString());
	}

	/**
	 * Authorized byte streaming. Two caller shapes share this route:
	 * <ul>
	 *   <li>an API client with a bearer JWT (no query parameters), and</li>
	 *   <li>a browser media element using a signed URL ({@code exp/uid/sig}),
	 *       which cannot carry a header.</li>
	 * </ul>
	 * Both resolve to a member before the same {@link MediaAccessService}
	 * check. {@code ?variant=thumbnail} serves the generated video thumbnail
	 * (image/jpeg) instead of the asset itself.
	 */
	@GetMapping("/api/v1/media/{id}/bytes")
	public ResponseEntity<byte[]> bytes(@PathVariable Long id,
			@RequestParam(required = false) String exp,
			@RequestParam(required = false) String uid,
			@RequestParam(required = false) String sig,
			@RequestParam(required = false) String variant) {
		User actor = exp == null && uid == null && sig == null
				? access.currentMember(currentUsername())
				: signedViewer(id, exp, uid, sig);
		MediaAsset asset = media.get(id);
		mediaAccess.assertCanView(actor, asset); // authorization: blocks/tombstones → 404
		boolean thumbnail = "thumbnail".equals(variant) && asset.getThumbnailKey() != null;
		String key = thumbnail ? asset.getThumbnailKey() : asset.getStorageKey();
		String mimeType = thumbnail ? "image/jpeg" : asset.getMimeType();
		try (InputStream in = media.read(key).stream()) {
			return ResponseEntity.ok()
					.header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
					.contentType(MediaType.parseMediaType(mimeType))
					.body(in.readAllBytes());
		}
		catch (IOException e) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to stream media");
		}
	}

	/** Resolve and validate the viewer encoded in a signed media URL. */
	private User signedViewer(Long id, String exp, String uid, String sig) {
		long viewerId = mediaUrls.validate(id, exp, uid, sig);
		User viewer = users.findById(viewerId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
		// A signed link is for one member only: a bearer caller must not replay
		// somebody else's link to reach bytes the signature alone would allow.
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
			User headerViewer = access.find(auth.getName()).orElse(null);
			if (headerViewer != null && !headerViewer.getId().equals(viewerId)) {
				throw new ApiException(HttpStatus.UNAUTHORIZED, "Media URL belongs to another member");
			}
		}
		return viewer;
	}

	private String currentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}
		return auth.getName();
	}
}
