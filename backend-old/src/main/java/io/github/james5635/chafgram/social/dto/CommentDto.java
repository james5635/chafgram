package io.github.james5635.chafgram.social.dto;

import io.github.james5635.chafgram.social.ReactionType;

import java.time.Instant;

public record CommentDto(
		Long id,
		MemberDto author,
		Long postId,
		Long parentId,
		String body,
		Instant editedAt,
		Instant createdAt,
		long reactionCount,
		ReactionType myReaction) {
}
