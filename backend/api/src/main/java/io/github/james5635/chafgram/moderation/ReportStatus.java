package io.github.james5635.chafgram.moderation;

import io.github.james5635.chafgram.common.utils.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReportStatus {
	OPEN, REVIEWING, ACTIONED, DISMISSED
}

@Converter
class ReportStatusConverter extends LowercaseEnumConverter<ReportStatus> {
	ReportStatusConverter() { super(ReportStatus.class); }
}
