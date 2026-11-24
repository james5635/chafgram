package io.github.james5635.chafgram.identity.dto;

import java.time.Instant;

public record InviteDto(
		Long id,
		String email,
		String role,
		Instant createdAt,
		Instant expiresAt,
		Instant acceptedAt) {
}
