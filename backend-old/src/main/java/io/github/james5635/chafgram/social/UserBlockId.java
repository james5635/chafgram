package io.github.james5635.chafgram.social;

import java.io.Serializable;
import java.util.Objects;

public class UserBlockId implements Serializable {
	private Long blockerId;
	private Long blockedId;

	public UserBlockId() {
	}

	public UserBlockId(Long blockerId, Long blockedId) {
		this.blockerId = blockerId;
		this.blockedId = blockedId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UserBlockId other)) {
			return false;
		}
		return Objects.equals(blockerId, other.blockerId) && Objects.equals(blockedId, other.blockedId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(blockerId, blockedId);
	}
}
