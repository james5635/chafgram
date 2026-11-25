package io.github.james5635.chafgram.notifications;

import io.github.james5635.chafgram.common.utils.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum NotificationEntityType {
	POST, COMMENT, MESSAGE, USER
}

@Converter
class NotificationEntityTypeConverter extends LowercaseEnumConverter<NotificationEntityType> {
	NotificationEntityTypeConverter() { super(NotificationEntityType.class); }
}
