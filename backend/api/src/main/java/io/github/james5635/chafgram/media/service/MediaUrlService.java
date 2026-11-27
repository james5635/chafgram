package io.github.james5635.chafgram.media.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.config.JwtConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Issues and verifies short-lived, viewer-bound URLs for media bytes.
 *
 * <p>{@code <img>} and {@code <video>} cannot carry the access JWT, so the
 * browser loads bytes through the BFF media route with an exp/uid/sig query
 * string. The signature is an HMAC-SHA256 over {@code id:exp:viewerId} using
 * the same secret as the access JWTs — signing a media URL never touches
 * refresh tokens. The BFF path is relative so the backend URL stays private.</p>
 */
@Service
public class MediaUrlService {

	private static final String URL_PREFIX = "/api/media/";

	private final byte[] secret;

	@Value("${chafgram.media.url-ttl:${CHAFGRAM_MEDIA_URL_TTL:1h}}")
	private Duration ttl;

	public MediaUrlService(JwtConfig jwtConfig) {
		this.secret = jwtConfig.signingSecret();
	}

	/** {@code url} is the signed BFF path; {@code expiresAt} is the same expiry. */
	public record SignedUrl(String url, Instant expiresAt) {
	}

	public String sign(Long mediaId, Long viewerId) {
		return issue(mediaId, viewerId).url();
	}

	public SignedUrl issue(Long mediaId, Long viewerId) {
		if (mediaId == null || viewerId == null) {
			throw invalid();
		}
		Instant expiresAt = Instant.now().plus(ttl);
		long exp = expiresAt.getEpochSecond();
		String sig = signature(mediaId, exp, viewerId);
		return new SignedUrl(URL_PREFIX + mediaId + "?exp=" + exp
				+ "&uid=" + viewerId + "&sig=" + sig, expiresAt);
	}

	/** Verify a signed URL and return the viewer id it was issued for. */
	public Long validate(Long mediaId, String exp, String uid, String sig) {
		if (mediaId == null || exp == null || uid == null || sig == null) {
			throw invalid();
		}
		long expSeconds;
		long viewerId;
		try {
			expSeconds = Long.parseLong(exp);
			viewerId = Long.parseLong(uid);
		}
		catch (NumberFormatException e) {
			throw invalid();
		}
		if (!Instant.ofEpochSecond(expSeconds).isAfter(Instant.now())) {
			throw invalid();
		}
		byte[] expected = signature(mediaId, expSeconds, viewerId).getBytes(StandardCharsets.UTF_8);
		byte[] supplied = sig.getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expected, supplied)) {
			throw invalid();
		}
		return viewerId;
	}

	private String signature(Long mediaId, long exp, Long viewerId) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			String message = mediaId + ":" + exp + ":" + viewerId;
			return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception e) {
			throw new IllegalStateException("Cannot sign media URL", e);
		}
	}

	private static ApiException invalid() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired media URL");
	}
}
