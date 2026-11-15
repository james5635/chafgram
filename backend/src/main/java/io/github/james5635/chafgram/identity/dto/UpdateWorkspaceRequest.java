package io.github.james5635.chafgram.identity.dto;

/** Any null field is left unchanged. */
public record UpdateWorkspaceRequest(
		String name,
		String slug,
		String primaryColor,
		String signupMode) {
}
