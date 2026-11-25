package io.github.james5635.chafgram.social;

import java.io.Serializable;
import java.util.Objects;

public class PostMediaId implements Serializable {
	private Long postId;
	private Long mediaId;

	public PostMediaId() {
	}

	public PostMediaId(Long postId, Long mediaId) {
		this.postId = postId;
		this.mediaId = mediaId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PostMediaId other)) {
			return false;
		}
		return Objects.equals(postId, other.postId) && Objects.equals(mediaId, other.mediaId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(postId, mediaId);
	}
}
