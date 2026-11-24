package io.github.james5635.chafgram.social.dto;

import io.github.james5635.chafgram.social.ReactionType;

public record ReactionSummaryDto(long count, ReactionType myReaction) {
}
