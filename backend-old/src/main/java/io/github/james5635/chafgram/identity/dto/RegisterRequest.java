package io.github.james5635.chafgram.identity.dto;

/** Self-serve registration. Only available when workspace signup_mode is `open`. */
public record RegisterRequest(
		String username,
		String displayName,
		String email,
		String password) {
}
