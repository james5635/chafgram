package io.github.james5635.chafgram.media.dto;

import java.time.Instant;

public record MediaAssetDto(
		Long id,
		String kind,
		String status,
		Long ownerId,
		String mimeType,
		Integer width,
		Integer height,
		String sizeBytes,
		Instant createdAt) {
}
