package io.github.james5635.chafgram.moderation;

import io.github.james5635.chafgram.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReportReason {
	SPAM, HARASSMENT, NSFW, VIOLENCE, IMPERSONATION, OTHER
}

@Converter
class ReportReasonConverter extends LowercaseEnumConverter<ReportReason> {
	ReportReasonConverter() { super(ReportReason.class); }
}
