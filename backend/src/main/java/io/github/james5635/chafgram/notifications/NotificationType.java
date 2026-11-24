package io.github.james5635.chafgram.notifications;

import io.github.james5635.chafgram.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum NotificationType {
	FOLLOW,
	LIKE,
	COMMENT,
	REPLY,
	MENTION,
	MESSAGE,
	SYSTEM
}

@Converter
class NotificationTypeConverter extends LowercaseEnumConverter<NotificationType> {
	NotificationTypeConverter() { super(NotificationType.class); }
}
