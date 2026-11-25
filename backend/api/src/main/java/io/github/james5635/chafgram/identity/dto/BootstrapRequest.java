package io.github.james5635.chafgram.identity.dto;

/**
 * One-time first-run setup. Only valid while the workspace has no members;
 * the created account becomes the first admin.
 */
public record BootstrapRequest(
		String username,
		String displayName,
		String email,
		String password) {
}
