package io.github.james5635.chafgram.notifications;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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

	/** How many entity ids one bulk visibility query may carry. */
	private static final int VISIBILITY_CHUNK = 500;

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

	/**
	 * Visible notifications, newest first. The list has no cursor, so a page
	 * full of hidden targets must not swallow the older visible ones — keep
	 * scanning until the requested number of visible rows is collected.
	 */
	public List<Notification> list(Long recipientId, int first) {
		int limit = Math.min(Math.max(first, 1), 100);
		List<Notification> visible = new ArrayList<>();
		int page = 0;
		while (visible.size() < limit) {
			List<Notification> chunk = notifications.findByRecipient(recipientId, PageRequest.of(page, limit));
			if (chunk.isEmpty()) {
				break;
			}
			Visibility visibility = visibilityOf(chunk);
			for (Notification n : chunk) {
				if (visible.size() >= limit) {
					break;
				}
				if (isVisible(n, visibility)) {
					visible.add(n);
				}
			}
			page++;
		}
		return visible;
	}

	/**
	 * Unread count over the visible notifications. Visibility is resolved in
	 * bulk (one query per entity type) instead of one query per row — the
	 * unread set is unbounded, so a per-row check would be an N+1.
	 */
	public long unreadCount(Long recipientId) {
		List<Notification> unread = notifications.findUnreadByRecipient(recipientId);
		if (unread.isEmpty()) {
			return 0;
		}
		Visibility visibility = visibilityOf(unread);
		return unread.stream().filter(n -> isVisible(n, visibility)).count();
	}

	/** Visible entity ids for a batch of notifications. */
	private Visibility visibilityOf(List<Notification> batch) {
		List<Long> postIds = entityIds(batch, NotificationEntityType.POST);
		List<Long> commentIds = entityIds(batch, NotificationEntityType.COMMENT);
		List<Long> messageIds = entityIds(batch, NotificationEntityType.MESSAGE);
		return new Visibility(
				visibleIds(postIds, posts::findVisibleIds),
				visibleIds(commentIds, comments::findVisibleIds),
				visibleIds(messageIds, messages::findVisibleIds));
	}

	/**
	 * Runs the bulk lookup in chunks — a single {@code IN} list with tens of
	 * thousands of ids would be a monster query on the unread set.
	 */
	private static Set<Long> visibleIds(List<Long> ids,
			Function<List<Long>, List<Long>> lookup) {
		if (ids.isEmpty()) {
			return Set.of();
		}
		Set<Long> visible = new HashSet<>();
		for (int start = 0; start < ids.size(); start += VISIBILITY_CHUNK) {
			List<Long> chunk = ids.subList(start, Math.min(ids.size(), start + VISIBILITY_CHUNK));
			visible.addAll(lookup.apply(chunk));
		}
		return visible;
	}

	private List<Long> entityIds(List<Notification> batch, NotificationEntityType type) {
		return batch.stream()
				.filter(n -> n.getEntityType() == type && n.getEntityId() != null)
				.map(Notification::getEntityId)
				.distinct()
				.toList();
	}

	private boolean isVisible(Notification n, Visibility visibility) {
		if (n.getEntityType() == null || n.getEntityId() == null) {
			return true;
		}
		switch (n.getEntityType()) {
			case POST:
				return visibility.posts().contains(n.getEntityId());
			case COMMENT:
				return visibility.comments().contains(n.getEntityId());
			case MESSAGE:
				return visibility.messages().contains(n.getEntityId());
			default:
				return true;
		}
	}

	private record Visibility(Set<Long> posts, Set<Long> comments, Set<Long> messages) {}

	public void mute(Long userId, NotificationType type) {
		muteStore.mute(userId, type);
	}

	public void unmute(Long userId, NotificationType type) {
		muteStore.unmute(userId, type);
	}
}
