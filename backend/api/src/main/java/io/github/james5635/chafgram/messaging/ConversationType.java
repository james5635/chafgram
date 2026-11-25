package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.common.utils.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ConversationType {
	DIRECT, GROUP
}

@Converter
class ConversationTypeConverter extends LowercaseEnumConverter<ConversationType> {
	ConversationTypeConverter() { super(ConversationType.class); }
}
