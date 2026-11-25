package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.common.utils.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MediaKind {
	IMAGE, VIDEO
}

@Converter
class MediaKindConverter extends LowercaseEnumConverter<MediaKind> {
	MediaKindConverter() { super(MediaKind.class); }
}
