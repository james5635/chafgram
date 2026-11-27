package io.github.james5635.chafgram.identity.dto;

/**
 * Claiming an invite. `email` may be omitted when the invite is email-bound;
 * it is required for reusable links. `password` is optional at the API level,
 * but an account without one cannot sign in with a password.
 */
public record AcceptInviteRequest(
		String token,
		String username,
		String displayName,
		String email,
		String password) {
}
