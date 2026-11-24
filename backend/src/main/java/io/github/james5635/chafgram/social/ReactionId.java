package io.github.james5635.chafgram.social;

import java.io.Serializable;
import java.util.Objects;

public class ReactionId implements Serializable {
	private Long postId;
	private Long userId;

	public ReactionId() {
	}

	public ReactionId(Long postId, Long userId) {
		this.postId = postId;
		this.userId = userId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReactionId other)) {
			return false;
		}
		return Objects.equals(postId, other.postId) && Objects.equals(userId, other.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(postId, userId);
	}
}
