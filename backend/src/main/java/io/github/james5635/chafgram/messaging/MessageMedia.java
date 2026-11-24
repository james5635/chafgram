package io.github.james5635.chafgram.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "message_media")
@IdClass(MessageMediaId.class)
@Getter
@Setter
@NoArgsConstructor
public class MessageMedia {

	@Id
	@Column(name = "message_id")
	private Long messageId;

	@Id
	@Column(name = "media_id")
	private Long mediaId;

	@Column(nullable = false)
	private Short position;
}
