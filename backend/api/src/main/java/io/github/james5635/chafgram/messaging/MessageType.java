package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.common.utils.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MessageType {
	TEXT, IMAGE, VIDEO, FILE, SYSTEM
}

@Converter
class MessageTypeConverter extends LowercaseEnumConverter<MessageType> {
	MessageTypeConverter() { super(MessageType.class); }
}
