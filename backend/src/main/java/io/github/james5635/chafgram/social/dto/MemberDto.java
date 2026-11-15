package io.github.james5635.chafgram.social.dto;

import java.time.Instant;

public record MemberDto(
		Long id,
		String username,
		String displayName,
		String bio,
		Long avatarMediaId,
		String role,
		String status,
		Instant createdAt,
		long followerCount,
		long followingCount,
		boolean isFollowing) {
}
