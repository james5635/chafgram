package io.github.james5635.chafgram.moderation;

import io.github.james5635.chafgram.common.utils.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReportTargetType {
	POST, COMMENT, MESSAGE, USER
}

@Converter
class ReportTargetTypeConverter extends LowercaseEnumConverter<ReportTargetType> {
	ReportTargetTypeConverter() { super(ReportTargetType.class); }
}
