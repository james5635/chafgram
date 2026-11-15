package io.github.james5635.chafgram.moderation.dto;

import java.time.Instant;

public record AuditLogEntryDto(
		Long id,
		Long actorId,
		String action,
		String targetType,
		Long targetId,
		Instant createdAt) {
}
