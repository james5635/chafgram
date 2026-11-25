package io.github.james5635.chafgram.social.dto;

import io.github.james5635.chafgram.notifications.NotificationType;

import java.time.Instant;

public record NotificationDto(
		Long id,
		MemberDto actor,
		NotificationType type,
		String entityType,
		Long entityId,
		Instant readAt,
		Instant createdAt) {
}
