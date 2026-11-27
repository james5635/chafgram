package io.github.james5635.chafgram.social.dto;

import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.ReactionType;

import java.time.Instant;
import java.util.List;

public record PostDto(
		Long id,
		MemberDto author,
		PostType type,
		String body,
		Instant editedAt,
		Instant createdAt,
		long commentCount,
		long reactionCount,
		ReactionType myReaction,
		boolean isSaved,
		List<Long> mediaIds,
		List<String> mediaUrls) {
}
