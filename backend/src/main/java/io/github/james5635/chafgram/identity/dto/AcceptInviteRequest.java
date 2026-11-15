package io.github.james5635.chafgram.identity.dto;

/**
 * Claiming an invite. `email` may be omitted when the invite is email-bound;
 * it is required for reusable links. `password` is optional (passkey-only
 * accounts are supported) but strongly recommended as a fallback.
 */
public record AcceptInviteRequest(
		String token,
		String username,
		String displayName,
		String email,
		String password) {
}
