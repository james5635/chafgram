package io.github.james5635.chafgram.moderation.dto;

public record ReportRequest(String targetType, Long targetId, String reason, String details) {
}
