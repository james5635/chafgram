package io.github.james5635.chafgram.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "conversation_members")
@IdClass(ConversationMemberId.class)
@Getter
@Setter
@NoArgsConstructor
public class ConversationMember {

	@Id
	@Column(name = "conversation_id")
	private Long conversationId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Convert(converter = ConversationRoleConverter.class)
	@Column(nullable = false, length = 6)
	private ConversationRole role = ConversationRole.MEMBER;

	@CreationTimestamp
	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt;

	@Column(name = "left_at")
	private Instant leftAt;

	@Column(name = "last_read_message_id")
	private Long lastReadMessageId;

	@Column(name = "muted_until")
	private Instant mutedUntil;
}
