package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReactionType {
	LIKE,
	LOVE,
	HAHA,
	WOW,
	SAD,
	ANGRY
}

@Converter
class ReactionTypeConverter extends LowercaseEnumConverter<ReactionType> {
	ReactionTypeConverter() { super(ReactionType.class); }
}
