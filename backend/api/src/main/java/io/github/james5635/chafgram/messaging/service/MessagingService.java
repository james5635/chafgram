package io.github.james5635.chafgram.messaging.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.UserStatus;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.service.MediaService;
import io.github.james5635.chafgram.media.MediaStatus;
import io.github.james5635.chafgram.media.utils.MediaStorage;
import io.github.james5635.chafgram.media.service.MediaValidation;
import io.github.james5635.chafgram.notifications.NotificationEntityType;
import io.github.james5635.chafgram.notifications.service.NotificationService;
import io.github.james5635.chafgram.notifications.NotificationType;
import io.github.james5635.chafgram.social.service.BlockService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import io.github.james5635.chafgram.messaging.repository.ConversationMemberRepository;
import io.github.james5635.chafgram.messaging.repository.ConversationRepository;
import io.github.james5635.chafgram.messaging.repository.MessageMediaRepository;
import io.github.james5635.chafgram.messaging.repository.MessageRepository;
import io.github.james5635.chafgram.messaging.Conversation;
import io.github.james5635.chafgram.messaging.ConversationMember;
import io.github.james5635.chafgram.messaging.ConversationRole;
import io.github.james5635.chafgram.messaging.ConversationType;
import io.github.james5635.chafgram.messaging.Message;
import io.github.james5635.chafgram.messaging.MessageMedia;
import io.github.james5635.chafgram.messaging.MessageType;

/**
 * 1:1 and group conversations. Direct chats are created on first message;
 * blocked members cannot message each other (BR-6.2). Unread counts derive
 * from `last_read_message_id` — no per-message receipts (BR-6.6).
 * Media messages (FR-5.4, FR-6.6) are attached via `message_media` with
 * ownership and status validation.
 */
@Service
public class MessagingService {

	private final ConversationRepository conversations;
	private final ConversationMemberRepository members;
	private final MessageRepository messages;
	private final MessageMediaRepository messageMedia;
	private final UserRepository users;
	private final BlockService blocks;
	private final NotificationService notifications;
	private final MediaValidation mediaValidation;
	private final MediaAssetRepository mediaAssets;
	private final MediaStorage mediaStorage;
	private final MediaService media;

	public MessagingService(ConversationRepository conversations, ConversationMemberRepository members,
			MessageRepository messages, MessageMediaRepository messageMedia, UserRepository users,
			BlockService blocks, NotificationService notifications, MediaValidation mediaValidation,
			MediaAssetRepository mediaAssets, MediaStorage mediaStorage, MediaService media) {
		this.conversations = conversations;
		this.members = members;
		this.messages = messages;
		this.messageMedia = messageMedia;
		this.users = users;
		this.blocks = blocks;
		this.notifications = notifications;
		this.mediaValidation = mediaValidation;
		this.mediaAssets = mediaAssets;
		this.mediaStorage = mediaStorage;
		this.media = media;
	}

	/**
	 * Uploads an image for immediate attachment to a message.
	 * <p>
	 * Videos are rejected: they go through the async transcode pipeline
	 * (FR-5.2), so they cannot be {@code ready} in the same request. The
	 * supported flow is upload → poll {@code mediaAsset(id)} until
	 * {@code status == "ready"} → send the message with {@code mediaIds}.
	 */
	public MediaAsset uploadMediaForMessage(User actor, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "A file is required");
		}
		String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
		MediaKind kind = kindOf(mime, file);
		if (kind == MediaKind.VIDEO) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"Videos are processed asynchronously — upload the file to POST /api/v1/media, "
							+ "wait until its status is 'ready', then send the message with mediaIds");
		}
		long size = file.getSize();
		if (kind == MediaKind.IMAGE && size > 20L * 1024 * 1024) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Images are limited to 20 MB");
		}

		String extension = extensionOf(file.getOriginalFilename());
		String key = mediaStorage.store(file, extension);

		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(actor.getId());
		asset.setKind(kind);
		asset.setStatus(MediaStatus.READY);
		asset.setStorageKey(key);
		asset.setMimeType(mime.isEmpty() ? "application/octet-stream" : mime);
		asset.setSizeBytes(size);
		MediaAsset saved = mediaAssets.save(asset);
		// same metadata as POST /api/v1/media (FR-5.2)
		media.populateImageMetadata(saved);
		return saved;
	}

	@Transactional
	public Message send(User actor, Long targetUserId, String body, List<Long> mediaIds) {
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
		return persist(actor, conversation, body, mediaIds);
	}

	@Transactional
	public Message sendToConversation(User actor, Long conversationId, String body, List<Long> mediaIds) {
		Conversation conversation = findAccessible(actor, conversationId);
		requireWriteable(actor);
		return persist(actor, conversation, body, mediaIds);
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

	public Conversation findAccessible(User actor, Long conversationId) {
		return conversations.findAccessibleById(conversationId, actor.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found or not a member"));
	}

	private void requireWriteable(User actor) {
		// suspended members can read but not write (functional-requirements.md FR-1.4)
		if (actor.getStatus() != UserStatus.ACTIVE) {
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

	private Message persist(User actor, Conversation conversation, String body, List<Long> mediaIds) {
		// Validate and attach media if provided
		List<MediaAsset> media = List.of();
		if (mediaIds != null && !mediaIds.isEmpty()) {
			if (mediaIds.size() > 10) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "At most 10 media items can be attached");
			}
			media = mediaValidation.validateAttachable(
					actor, mediaIds, 10, MediaKind.IMAGE, MediaKind.VIDEO);
			// Validate body length only if no media
			if ((body == null || body.isBlank()) && media.isEmpty()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Message body is required when no media attached");
			}
			if (body != null && body.length() > 5000) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Message body is too long");
			}
		} else {
			if (body == null || body.isBlank()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Message body is required");
			}
			if (body.length() > 5000) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Message body is too long");
			}
		}

		Message message = new Message();
		message.setConversationId(conversation.getId());
		message.setSenderId(actor.getId());
		message.setBody(body == null || body.isBlank() ? null : body.trim());

		// Determine message type based on media
		if (!media.isEmpty()) {
			boolean hasVideo = media.stream().anyMatch(m -> m.getKind() == MediaKind.VIDEO);
			boolean hasImage = media.stream().anyMatch(m -> m.getKind() == MediaKind.IMAGE);
			if (hasVideo) {
				message.setType(MessageType.VIDEO);
			} else if (hasImage) {
				message.setType(MessageType.IMAGE);
			} else {
				message.setType(MessageType.FILE);
			}
		} else {
			message.setType(MessageType.TEXT);
		}

		message.setBody(body == null || body.isBlank() ? null : body.trim());
		Message saved = messages.save(message);

		// Attach media
		if (!media.isEmpty()) {
			for (int i = 0; i < media.size(); i++) {
				MessageMedia mm = new MessageMedia();
				mm.setMessageId(saved.getId());
				mm.setMediaId(media.get(i).getId());
				mm.setPosition((short) i);
				messageMedia.save(mm);
			}
		}

		conversation.setUpdatedAt(Instant.now());
		conversations.save(conversation);

		for (ConversationMember member : members.findActiveByConversation(conversation.getId())) {
			if (!member.getUserId().equals(actor.getId())) {
				notifications.notify(member.getUserId(), actor.getId(), NotificationType.MESSAGE, NotificationEntityType.MESSAGE, saved.getId());
			}
		}
		return saved;
	}

	private MediaKind kindOf(String mime, MultipartFile file) {
		if (mime.startsWith("image/")) {
			return MediaKind.IMAGE;
		}
		if (mime.startsWith("video/")) {
			return MediaKind.VIDEO;
		}
		throw new ApiException(HttpStatus.BAD_REQUEST, "Only image and video uploads are supported");
	}

	private String extensionOf(String filename) {
		if (filename == null) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot).toLowerCase();
	}
}