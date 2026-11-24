package io.github.james5635.chafgram.social;

import java.io.Serializable;
import java.util.Objects;

public class SavedPostId implements Serializable {
	private Long userId;
	private Long postId;

	public SavedPostId() {
	}

	public SavedPostId(Long userId, Long postId) {
		this.userId = userId;
		this.postId = postId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SavedPostId other)) {
			return false;
		}
		return Objects.equals(userId, other.userId) && Objects.equals(postId, other.postId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, postId);
	}
}
