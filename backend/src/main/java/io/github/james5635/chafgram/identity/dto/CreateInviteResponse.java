package io.github.james5635.chafgram.identity.dto;

import java.time.Instant;

public record CreateInviteResponse(
		Long id,
		String email,
		String role,
		String token,
		String acceptPath,
		Instant expiresAt) {
}
