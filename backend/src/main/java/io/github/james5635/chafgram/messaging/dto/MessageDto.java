package io.github.james5635.chafgram.messaging.dto;

import java.time.Instant;

public record MessageDto(
		Long id,
		Long conversationId,
		Long senderId,
		String type,
		String body,
		Instant createdAt) {
}
