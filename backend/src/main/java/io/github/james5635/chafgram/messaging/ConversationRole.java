package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ConversationRole {
	MEMBER, ADMIN
}

@Converter
class ConversationRoleConverter extends LowercaseEnumConverter<ConversationRole> {
	ConversationRoleConverter() { super(ConversationRole.class); }
}
