package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.mail.ChafgramMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Email verification and password reset (FR-1.7).
 * <p>
 * Both flows use single-use, 30-minute tokens whose SHA-256 hash is all that
 * is stored. The raw token only ever exists in the mail body.
 */
@Service
public class AccountFlowService {

	private final UserRepository users;
	private final AccountTokenService accountTokens;
	private final ChafgramMailSender mail;
	private final PasswordEncoder passwordEncoder;
	private final String appUrl;

	public AccountFlowService(UserRepository users, AccountTokenService accountTokens,
			ChafgramMailSender mail, PasswordEncoder passwordEncoder,
			@Value("${chafgram.app-url:http://localhost:3000}") String appUrl) {
		this.users = users;
		this.accountTokens = accountTokens;
		this.mail = mail;
		this.passwordEncoder = passwordEncoder;
		this.appUrl = appUrl;
	}

	/**
	 * Starts a password reset. Always reports success — an unknown address
	 * must not be distinguishable from a known one.
	 */
	@Transactional
	public void requestPasswordReset(String email) {
		if (email == null || email.isBlank()) {
			return;
		}
		String normalized = email.trim().toLowerCase();
		User user = users.findByEmail(normalized).orElse(null);
		if (user == null || user.getStatus() == UserStatus.DEACTIVATED) {
			return;
		}
		String token = accountTokens.create(user.getId(), AccountTokenType.RESET_PASSWORD);
		mail.send(user.getEmail(), "Reset your Chafgram password",
				"Someone asked to reset the password for this Chafgram account.\n\n"
						+ appUrl + "/reset-password?token=" + token + "\n\n"
						+ "The link is valid for 30 minutes and can be used once. "
						+ "If you did not ask for this, you can ignore this message.");
	}

	/**
	 * Completes a password reset: validates the token, then replaces the
	 * password hash. A reset does not imply email verification.
	 */
	@Transactional
	public void resetPassword(String token, String password) {
		String normalizedPassword = Validation.normalizePassword(password);
		if (normalizedPassword == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required");
		}
		Long userId = accountTokens.consume(token, AccountTokenType.RESET_PASSWORD);
		User user = users.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token is invalid or has expired"));
		user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
		users.save(user);
	}

	/**
	 * Sends a verification link to the signed-in member's own address.
	 * Does nothing if the address is already verified.
	 */
	@Transactional
	public void requestEmailVerification(User actor) {
		if (actor.getEmailVerifiedAt() != null) {
			return;
		}
		String token = accountTokens.create(actor.getId(), AccountTokenType.VERIFY_EMAIL);
		mail.send(actor.getEmail(), "Verify your Chafgram email address",
				"Please confirm this email address for your Chafgram account.\n\n"
						+ appUrl + "/verify-email?token=" + token + "\n\n"
						+ "The link is valid for 30 minutes and can be used once.");
	}

	/**
	 * Marks the member's address as verified.
	 */
	@Transactional
	public void verifyEmail(String token) {
		Long userId = accountTokens.consume(token, AccountTokenType.VERIFY_EMAIL);
		User user = users.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token is invalid or has expired"));
		user.setEmailVerifiedAt(Instant.now());
		users.save(user);
	}
}
