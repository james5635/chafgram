package io.github.james5635.chafgram.messaging.dto;

import io.github.james5635.chafgram.media.dto.MediaAssetDto;
import java.time.Instant;
import java.util.List;

public record MessageDto(
		Long id,
		Long conversationId,
		Long senderId,
		String type,
		String body,
		List<MediaAssetDto> media,
		Instant createdAt) {
}
