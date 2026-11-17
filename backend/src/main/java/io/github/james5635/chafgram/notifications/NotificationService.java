package io.github.james5635.chafgram.notifications;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.social.PostRepository;
import io.github.james5635.chafgram.social.CommentRepository;
import io.github.james5635.chafgram.messaging.MessageRepository;
import org.springframework.http.HttpStatus;

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
	private final PostRepository posts;
	private final CommentRepository comments;
	private final MessageRepository messages;

	public NotificationService(NotificationRepository notifications, NotificationMuteStore muteStore,
			PostRepository posts, CommentRepository comments, MessageRepository messages) {
		this.notifications = notifications;
		this.muteStore = muteStore;
		this.posts = posts;
		this.comments = comments;
		this.messages = messages;
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

	@Transactional
	public void markRead(User actor, Long id) {
		Notification n = notifications.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
		if (!n.getRecipientId().equals(actor.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
		}
		if (n.getReadAt() != null) {
			return;
		}
		n.setReadAt(Instant.now());
		notifications.save(n);
	}

	@Transactional(readOnly = true)
	public List<Notification> listVisible(User actor, int first) {
		return list(actor.getId(), first);
	}

	@Transactional(readOnly = true)
	public long countUnreadVisible(User actor) {
		return unreadCount(actor.getId());
	}

	private boolean isVisible(Notification n) {
		if (n.getEntityType() == null || n.getEntityId() == null) {
			return true;
		}
		switch (n.getEntityType()) {
			case POST:
				return posts.countVisiblePost(n.getEntityId()) > 0;
			case COMMENT:
				return comments.countVisibleComment(n.getEntityId()) > 0;
			case MESSAGE:
				return messages.countVisibleMessage(n.getEntityId()) > 0;
			default:
				return true;
		}
	}

	public List<Notification> list(Long recipientId, int first) {
		return notifications.findByRecipient(recipientId, PageRequest.of(0, Math.min(Math.max(first, 1), 100)))
				.stream()
				.filter(n -> isVisible(n))
				.limit(Math.max(first, 1))
				.collect(Collectors.toList());
	}

	public long unreadCount(Long recipientId) {
		return notifications.findUnreadByRecipient(recipientId).stream()
				.filter(n -> isVisible(n))
				.count();
	}

	public void mute(Long userId, NotificationType type) {
		muteStore.mute(userId, type);
	}

	public void unmute(Long userId, NotificationType type) {
		muteStore.unmute(userId, type);
	}
}
