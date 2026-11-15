package io.github.james5635.chafgram.messaging.dto;

import java.time.Instant;

public record ConversationDto(
		Long id,
		String type,
		String title,
		Instant updatedAt) {
}
