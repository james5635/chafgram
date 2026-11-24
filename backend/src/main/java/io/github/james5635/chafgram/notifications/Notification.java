package io.github.james5635.chafgram.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "recipient_id", nullable = false)
	private Long recipientId;

	@Column(name = "actor_id")
	private Long actorId;

	@Convert(converter = NotificationTypeConverter.class)
	@Column(nullable = false, length = 7)
	private NotificationType type;

	@Convert(converter = NotificationEntityTypeConverter.class)
	@Column(name = "entity_type", length = 7)
	private NotificationEntityType entityType;

	@Column(name = "entity_id")
	private Long entityId;

	@Column(name = "read_at")
	private Instant readAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
