package io.github.james5635.chafgram.moderation.dto;

import java.time.Instant;

public record ReportDto(
		Long id,
		Long reporterId,
		String targetType,
		Long targetId,
		String reason,
		String status,
		Long resolvedBy,
		Instant resolvedAt,
		Instant createdAt) {
}
