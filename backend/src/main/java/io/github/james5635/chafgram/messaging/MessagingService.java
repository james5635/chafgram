package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserRepository;
import io.github.james5635.chafgram.notifications.NotificationEntityType;
import io.github.james5635.chafgram.notifications.NotificationService;
import io.github.james5635.chafgram.notifications.NotificationType;
import io.github.james5635.chafgram.social.BlockService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 1:1 and group conversations. Direct chats are created on first message;
 * blocked members cannot message each other (BR-6.2). Unread counts derive
 * from `last_read_message_id` — no per-message receipts (BR-6.6).
 */
@Service
public class MessagingService {

	private final ConversationRepository conversations;
	private final ConversationMemberRepository members;
	private final MessageRepository messages;
	private final UserRepository users;
	private final BlockService blocks;
	private final NotificationService notifications;

	public MessagingService(ConversationRepository conversations, ConversationMemberRepository members,
			MessageRepository messages, UserRepository users, BlockService blocks, NotificationService notifications) {
		this.conversations = conversations;
		this.members = members;
		this.messages = messages;
		this.users = users;
		this.blocks = blocks;
		this.notifications = notifications;
	}

	@Transactional
	public Message send(User actor, Long targetUserId, String body) {
		if (body == null || body.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Message body is required");
		}
		if (body.length() > 5000) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Message body is too long");
		}
		requireWriteable(actor);
		User target = users.findById(targetUserId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found"));
		if (target.getId().equals(actor.getId())) {
			throw new ApiException(HttpStatus.CONFLICT, "You cannot message yourself");
		}
		if (blocks.blocks(actor.getId(), target.getId()) || blocks.blocks(target.getId(), actor.getId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You cannot message this member"); // BR-6.2
		}
		Conversation conversation = conversations.findDirectBetween(actor.getId(), target.getId())
				.orElseGet(() -> createDirect(actor, target));
		return persist(actor, conversation, body);
	}

	@Transactional
	public Message sendToConversation(User actor, Long conversationId, String body) {
		Conversation conversation = findAccessible(actor, conversationId);
		requireWriteable(actor);
		return persist(actor, conversation, body);
	}

	@Transactional
	public Conversation createGroup(User actor, String title, List<Long> memberIds) {
		requireWriteable(actor);
		if (title == null || title.isBlank() || title.length() > 80) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Group title is required (max 80 chars)");
		}
		Conversation conversation = new Conversation();
		conversation.setType(ConversationType.GROUP);
		conversation.setTitle(title.trim());
		conversation.setCreatedBy(actor.getId());
		conversation = conversations.save(conversation);

		addMember(conversation.getId(), actor.getId(), ConversationRole.ADMIN);
		for (Long memberId : memberIds) {
			if (!members.findByConversationAndUser(conversation.getId(), memberId).isPresent()) {
				addMember(conversation.getId(), memberId, ConversationRole.MEMBER);
			}
		}
		return conversation;
	}

	@Transactional
	public void addGroupMember(User actor, Long conversationId, Long newMemberId, ConversationRole role) {
		findAccessible(actor, conversationId);
		requireGroupAdmin(actor, conversationId);
		if (!users.existsById(newMemberId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Member not found");
		}
		if (members.findByConversationAndUser(conversationId, newMemberId).isEmpty()) {
			addMember(conversationId, newMemberId, role == null ? ConversationRole.MEMBER : role);
		}
	}

	@Transactional
	public void removeGroupMember(User actor, Long conversationId, Long targetId) {
		findAccessible(actor, conversationId);
		requireGroupAdmin(actor, conversationId);
		if (actor.getId().equals(targetId)) {
			throw new ApiException(HttpStatus.CONFLICT, "Group admins cannot remove themselves; transfer or disband instead");
		}
		members.findByConversationAndUser(conversationId, targetId).ifPresent(m -> {
			m.setLeftAt(Instant.now());
			members.save(m);
		});
	}

	@Transactional
	public void leave(User actor, Long conversationId) {
		findAccessible(actor, conversationId);
		members.findByConversationAndUser(conversationId, actor.getId()).ifPresent(m -> {
			m.setLeftAt(Instant.now());
			members.save(m);
		});
	}

	@Transactional
	public void mute(User actor, Long conversationId, Instant until) {
		findAccessible(actor, conversationId);
		ConversationMember member = members.findByConversationAndUser(conversationId, actor.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not a member of this conversation"));
		member.setMutedUntil(until);
		members.save(member);
	}

	@Transactional
	public void markRead(User actor, Long conversationId) {
		findAccessible(actor, conversationId);
		ConversationMember member = members.findByConversationAndUser(conversationId, actor.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not a member of this conversation"));
		var latest = messages.findLatestMessage(conversationId);
		member.setLastReadMessageId(latest.map(Message::getId).orElse(0L));
		members.save(member);
	}

	@Transactional(readOnly = true)
	public List<Conversation> list(User actor, int limit) {
		return conversations.findByMember(actor.getId(), PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
	}

	@Transactional(readOnly = true)
	public List<Message> messages(User actor, Long conversationId, long cursor, int limit) {
		findAccessible(actor, conversationId);
		return messages.findPage(conversationId, cursor, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
	}

	@Transactional(readOnly = true)
	public long unreadCount(User actor, Long conversationId) {
		ConversationMember member = members.findByConversationAndUser(conversationId, actor.getId()).orElse(null);
		if (member == null || member.getMutedUntil() != null && member.getMutedUntil().isAfter(Instant.now())) {
			return 0L; // not a member, or muted
		}
		long afterId = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
		return messages.countUnread(conversationId, afterId);
	}

	Conversation findAccessible(User actor, Long conversationId) {
		return conversations.findAccessibleById(conversationId, actor.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found or not a member"));
	}

	private void requireWriteable(User actor) {
		// suspended members can read but not write (plans.md FR-1.4)
		if (actor.getStatus() != io.github.james5635.chafgram.identity.UserStatus.ACTIVE) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Account is " + actor.getStatus().name().toLowerCase() + "; sending messages is not allowed");
		}
	}

	private void requireGroupAdmin(User actor, Long conversationId) {
		ConversationMember member = members.findByConversationAndUser(conversationId, actor.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not a member of this conversation"));
		if (member.getRole() != ConversationRole.ADMIN) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Requires group admin");
		}
	}

	private Conversation createDirect(User actor, User target) {
		Conversation conversation = new Conversation();
		conversation.setType(ConversationType.DIRECT);
		conversation = conversations.save(conversation);
		addMember(conversation.getId(), actor.getId(), ConversationRole.MEMBER);
		addMember(conversation.getId(), target.getId(), ConversationRole.MEMBER);
		return conversation;
	}

	private void addMember(Long conversationId, Long userId, ConversationRole role) {
		ConversationMember member = new ConversationMember();
		member.setConversationId(conversationId);
		member.setUserId(userId);
		member.setRole(role);
		members.save(member);
	}

	private Message persist(User actor, Conversation conversation, String body) {
		Message message = new Message();
		message.setConversationId(conversation.getId());
		message.setSenderId(actor.getId());
		message.setType(MessageType.TEXT);
		message.setBody(body.trim());
		messages.save(message);
		conversation.setUpdatedAt(Instant.now());
		conversations.save(conversation);

		for (ConversationMember member : members.findActiveByConversation(conversation.getId())) {
			if (!member.getUserId().equals(actor.getId())) {
				notifications.notify(member.getUserId(), actor.getId(), NotificationType.MESSAGE, NotificationEntityType.MESSAGE, message.getId());
			}
		}
		return message;
	}
}
