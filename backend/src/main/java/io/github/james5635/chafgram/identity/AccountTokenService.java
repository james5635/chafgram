package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Issues and redeems single-use, time-boxed account tokens (FR-1.7).
 * <p>
 * Only the SHA-256 hash of a token is persisted. The raw token is returned
 * exactly once, at creation time, and travels to the member by email.
 */
@Service
public class AccountTokenService {

	/** Both token kinds expire after 30 minutes. */
	static final Duration TOKEN_TTL = Duration.ofMinutes(30);

	private final AccountTokenRepository tokens;

	public AccountTokenService(AccountTokenRepository tokens) {
		this.tokens = tokens;
	}

	/**
	 * Creates a fresh token for {@code userId} and returns the raw value.
	 * The raw value is never stored — only its SHA-256 hash.
	 */
	@Transactional
	public String create(Long userId, AccountTokenType type) {
		String raw = Tokens.newBearerToken();
		AccountToken token = new AccountToken();
		token.setUserId(userId);
		token.setType(type);
		token.setTokenHash(Tokens.sha256Hex(raw));
		token.setExpiresAt(Instant.now().plus(TOKEN_TTL));
		tokens.save(token);
		return raw;
	}

	/**
	 * Redeems a token and returns the owning member's id.
	 *
	 * @throws ApiException 400 when the token is unknown, already used,
	 *         expired, or of the wrong type. Every branch reports the same
	 *         message so the endpoint cannot be used to probe for valid
	 *         tokens.
	 */
	@Transactional
	public Long consume(String rawToken, AccountTokenType expectedType) {
		if (rawToken == null || rawToken.isBlank()) {
			throw invalidToken();
		}
		AccountToken token = tokens.findByHash(Tokens.sha256Hex(rawToken.trim()))
				.orElseThrow(AccountTokenService::invalidToken);

		Instant now = Instant.now();
		if (token.getType() != expectedType || !token.isUsableAt(now)) {
			throw invalidToken();
		}
		token.setUsedAt(now);
		tokens.save(token);
		return token.getUserId();
	}

	private static ApiException invalidToken() {
		return new ApiException(HttpStatus.BAD_REQUEST, "Token is invalid or has expired");
	}
}
