package io.github.james5635.chafgram.identity.auth;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.config.JwtConfig;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserStatus;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.utils.Tokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Issues short-lived access JWTs and opaque, database-backed refresh tokens.
 *
 * <p>Roles in the JWT are lower-case enum names; the resource-server converter
 * maps them to {@code ROLE_*} authorities. Authorization still re-reads the
 * member's current status in the domain services, so a JWT's claims can be up
 * to one access TTL stale without reopening a write path for a demoted or
 * suspended member.</p>
 */
@Service
public class TokenService {

	private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

	private final JwtEncoder jwtEncoder;
	private final JwtConfig jwtConfig;
	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;

	@Value("${chafgram.access-ttl:${CHAFGRAM_ACCESS_TTL:15m}}")
	private Duration accessTtl;

	@Value("${chafgram.refresh-ttl:${CHAFGRAM_REFRESH_TTL:14d}}")
	private Duration refreshTtl;

	public TokenService(JwtEncoder jwtEncoder, JwtConfig jwtConfig, UserRepository users,
			RefreshTokenRepository refreshTokens) {
		this.jwtEncoder = jwtEncoder;
		this.jwtConfig = jwtConfig;
		this.users = users;
		this.refreshTokens = refreshTokens;
	}

	/** Mint a new access token and persist one refresh-token hash. */
	@Transactional
	public TokenPair issue(User user) {
		Instant now = Instant.now();
		String rawRefreshToken = Tokens.newBearerToken();
		storeRefreshToken(user.getId(), now, rawRefreshToken);
		return new TokenPair(accessToken(user, now), rawRefreshToken, accessTtl.toSeconds());
	}

	/**
	 * Rotate a refresh token. A row revoked with {@code replacedBy} was rotated
	 * (a lost response or a second tab) and is harmless; a row revoked without
	 * it was logged out or stolen, so the whole user's token family is revoked.
	 *
	 * <p>The auth failure must not roll back that family-wide revocation.</p>
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public TokenPair refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw invalidToken();
		}
		RefreshToken row = refreshTokens.findByTokenHash(Tokens.sha256Hex(rawRefreshToken))
				.orElseThrow(TokenService::invalidToken);
		Instant now = Instant.now();

		if (row.getRevokedAt() != null && row.getReplacedBy() == null) {
			revokeAllForUser(row.getUserId());
			throw invalidToken();
		}
		if (!row.getExpiresAt().isAfter(now)) {
			throw invalidToken();
		}
		User user = users.findById(row.getUserId()).orElseThrow(TokenService::invalidToken);
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw invalidToken();
		}

		String rawRefreshTokenNext = Tokens.newBearerToken();
		RefreshToken successor = storeRefreshToken(user.getId(), now, rawRefreshTokenNext);
		row.setRevokedAt(now);
		// Keep the first successor recorded when a superseded token is replayed.
		if (row.getReplacedBy() == null) {
			row.setReplacedBy(successor.getId());
		}
		refreshTokens.save(row);
		return new TokenPair(accessToken(user, now), rawRefreshTokenNext, accessTtl.toSeconds());
	}

	/** Logout: revoke just this token, leaving its row without a successor. */
	@Transactional
	public void revoke(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		refreshTokens.findByTokenHash(Tokens.sha256Hex(rawRefreshToken)).ifPresent(row -> {
			if (row.getRevokedAt() == null) {
				row.setRevokedAt(Instant.now());
				refreshTokens.save(row);
			}
		});
	}

	/** Revoke every live refresh token for a user (logout-everywhere / reuse detection). */
	@Transactional
	public void revokeAllForUser(Long userId) {
		refreshTokens.revokeAllForUser(userId, Instant.now());
	}

	private RefreshToken storeRefreshToken(Long userId, Instant now, String rawRefreshToken) {
		RefreshToken row = new RefreshToken();
		row.setUserId(userId);
		row.setTokenHash(Tokens.sha256Hex(rawRefreshToken));
		row.setExpiresAt(now.plus(refreshTtl));
		return refreshTokens.save(row);
	}

	private String accessToken(User user, Instant now) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtConfig.issuer())
				.audience(List.of(jwtConfig.audience()))
				.subject(String.valueOf(user.getId()))
				.issuedAt(now)
				.expiresAt(now.plus(accessTtl))
				.claim("roles", List.of(user.getRole().name().toLowerCase(Locale.ROOT)))
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	private static ApiException invalidToken() {
		return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
	}
}
