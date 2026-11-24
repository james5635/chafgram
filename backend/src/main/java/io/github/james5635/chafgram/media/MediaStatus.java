package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MediaStatus {
	PROCESSING, READY, FAILED, DELETED
}

@Converter
class MediaStatusConverter extends LowercaseEnumConverter<MediaStatus> {
	MediaStatusConverter() { super(MediaStatus.class); }
}
