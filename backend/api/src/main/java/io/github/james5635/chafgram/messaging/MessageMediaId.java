package io.github.james5635.chafgram.messaging;

import java.io.Serializable;
import java.util.Objects;

public class MessageMediaId implements Serializable {
	private Long messageId;
	private Long mediaId;

	public MessageMediaId() {
	}

	public MessageMediaId(Long messageId, Long mediaId) {
		this.messageId = messageId;
		this.mediaId = mediaId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MessageMediaId other)) {
			return false;
		}
		return Objects.equals(messageId, other.messageId) && Objects.equals(mediaId, other.mediaId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageId, mediaId);
	}
}
