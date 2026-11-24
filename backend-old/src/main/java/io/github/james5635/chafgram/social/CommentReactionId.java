package io.github.james5635.chafgram.social;

import java.io.Serializable;
import java.util.Objects;

public class CommentReactionId implements Serializable {
	private Long commentId;
	private Long userId;

	public CommentReactionId() {
	}

	public CommentReactionId(Long commentId, Long userId) {
		this.commentId = commentId;
		this.userId = userId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CommentReactionId other)) {
			return false;
		}
		return Objects.equals(commentId, other.commentId) && Objects.equals(userId, other.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(commentId, userId);
	}
}
