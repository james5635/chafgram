package io.github.james5635.chafgram.social;

import java.io.Serializable;
import java.util.Objects;

public class FollowId implements Serializable {
	private Long followerId;
	private Long followeeId;

	public FollowId() {
	}

	public FollowId(Long followerId, Long followeeId) {
		this.followerId = followerId;
		this.followeeId = followeeId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FollowId other)) {
			return false;
		}
		return Objects.equals(followerId, other.followerId) && Objects.equals(followeeId, other.followeeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(followerId, followeeId);
	}
}
