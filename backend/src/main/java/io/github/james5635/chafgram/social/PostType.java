package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum PostType {
	NOTE,
	PHOTO,
	VIDEO
}

@Converter
class PostTypeConverter extends LowercaseEnumConverter<PostType> {
	PostTypeConverter() { super(PostType.class); }
}
