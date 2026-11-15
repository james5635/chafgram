package io.github.james5635.chafgram.notifications;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * In-app notifications only — nothing is ever sent outside the company
 * instance (no email, no push). Write-time fan-out: one row per recipient.
 * Per-member mutes from `notification_preferences` are honored (presence = muted).
 */
@Service
@Transactional
public class NotificationService {

	private final NotificationRepository notifications;
	private final NotificationMuteStore muteStore;

	public NotificationService(NotificationRepository notifications, NotificationMuteStore muteStore) {
		this.notifications = notifications;
		this.muteStore = muteStore;
	}

	public void notify(Long recipientId, Long actorId, NotificationType type,
			NotificationEntityType entityType, Long entityId) {
		if (recipientId.equals(actorId)) {
			return;
		}
		if (muteStore.isMuted(recipientId, type)) {
			return;
		}
		Notification n = new Notification();
		n.setRecipientId(recipientId);
		n.setActorId(actorId);
		n.setType(type);
		n.setEntityType(entityType);
		n.setEntityId(entityId);
		notifications.save(n);
	}

	public void markAllRead(Long recipientId) {
		List<Notification> unread = notifications.findUnreadByRecipient(recipientId);
		Instant now = Instant.now();
		for (Notification n : unread) {
			n.setReadAt(now);
		}
		notifications.saveAll(unread);
	}

	public List<Notification> list(Long recipientId, int first) {
		return notifications.findByRecipient(recipientId, PageRequest.of(0, Math.min(Math.max(first, 1), 100)));
	}

	public long unreadCount(Long recipientId) {
		return notifications.countUnread(recipientId);
	}

	public void mute(Long userId, NotificationType type) {
		muteStore.mute(userId, type);
	}

	public void unmute(Long userId, NotificationType type) {
		muteStore.unmute(userId, type);
	}
}
