package io.github.james5635.chafgram.identity.dto;

import java.time.Instant;

public record MemberDetailDto(
		Long id,
		String username,
		String displayName,
		String email,
		String role,
		String status,
		Instant createdAt) {
}
