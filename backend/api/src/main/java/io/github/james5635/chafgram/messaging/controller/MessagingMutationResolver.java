package io.github.james5635.chafgram.messaging.controller;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.messaging.dto.ConversationDto;
import io.github.james5635.chafgram.messaging.dto.MessageDto;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.media.dto.MediaAssetDto;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.messaging.repository.MessageMediaRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import io.github.james5635.chafgram.messaging.service.MessagingService;
import io.github.james5635.chafgram.messaging.Conversation;
import io.github.james5635.chafgram.messaging.ConversationRole;
import io.github.james5635.chafgram.messaging.Message;

@Controller
@PreAuthorize("isFullyAuthenticated()")
public class MessagingMutationResolver {

	private final MessagingService messaging;
	private final MemberAccess access;
	private final CurrentMember current;
	private final MediaAssetRepository mediaAssets;
	private final MessageMediaRepository messageMedia;

	public MessagingMutationResolver(MessagingService messaging, MemberAccess access, CurrentMember current,
			MediaAssetRepository mediaAssets, MessageMediaRepository messageMedia) {
		this.messaging = messaging;
		this.access = access;
		this.current = current;
		this.mediaAssets = mediaAssets;
		this.messageMedia = messageMedia;
	}

	@MutationMapping
	public MessageDto sendDirectMessage(@Argument Long userId, @Argument String body, @Argument List<Long> mediaIds) {
		var actor = access.requireWriteable(current.require().getUsername());
		return toDto(messaging.send(actor, userId, body, mediaIds), mediaAssets, messageMedia);
	}

	@MutationMapping
	public MessageDto sendMessage(@Argument Long conversationId, @Argument String body, @Argument List<Long> mediaIds) {
		var actor = access.requireWriteable(current.require().getUsername());
		return toDto(messaging.sendToConversation(actor, conversationId, body, mediaIds), mediaAssets, messageMedia);
	}

	@MutationMapping
	public ConversationDto createGroupConversation(@Argument String title, @Argument List<Long> memberIds) {
		var actor = access.requireWriteable(current.require().getUsername());
		return toDto(messaging.createGroup(actor, title, memberIds == null ? List.of() : memberIds));
	}

	@MutationMapping
	public ConversationDto addGroupMember(@Argument Long id, @Argument Long userId, @Argument String role) {
		var actor = access.requireWriteable(current.require().getUsername());
		ConversationRole parsed = role == null || role.isBlank()
				? ConversationRole.MEMBER
				: ConversationRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
		messaging.addGroupMember(actor, id, userId, parsed);
		return toDto(messaging.findAccessible(actor, id));
	}

	@MutationMapping
	public ConversationDto removeGroupMember(@Argument Long id, @Argument Long userId) {
		var actor = access.requireWriteable(current.require().getUsername());
		messaging.removeGroupMember(actor, id, userId);
		return toDto(messaging.findAccessible(actor, id));
	}

	@MutationMapping
	public boolean leaveConversation(@Argument Long id) {
		var actor = access.requireWriteable(current.require().getUsername());
		messaging.leave(actor, id);
		return true;
	}

	@MutationMapping
	public boolean muteConversation(@Argument Long id, @Argument String until) {
		var actor = access.requireWriteable(current.require().getUsername());
		Instant parsed = until == null || until.isBlank() ? null : parseInstant(until);
		messaging.mute(actor, id, parsed);
		return true;
	}

	@MutationMapping
	public boolean markConversationRead(@Argument Long id) {
		var actor = access.requireWriteable(current.require().getUsername());
		messaging.markRead(actor, id);
		return true;
	}

	private static Instant parseInstant(String raw) {
		try {
			return Instant.parse(raw);
		}
		catch (DateTimeParseException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "until must be an ISO-8601 instant");
		}
	}

	private static ConversationDto toDto(Conversation c) {
		return new ConversationDto(c.getId(), c.getType().name().toLowerCase(), c.getTitle(), c.getUpdatedAt());
	}

	private static MessageDto toDto(Message m, MediaAssetRepository mediaAssets, MessageMediaRepository messageMedia) {
		Long senderId = m.getSenderId() == null ? -1L : m.getSenderId();
		List<MediaAssetDto> media = messageMedia.findByMessageId(m.getId()).stream()
				.map(mm -> mediaAssets.findById(mm.getMediaId()).orElse(null))
				.filter(Objects::nonNull)
				.map(ma -> new MediaAssetDto(ma.getId(), ma.getKind().name().toLowerCase(), ma.getStatus().name().toLowerCase(),
						ma.getOwnerUserId(), ma.getMimeType(), ma.getWidth(), ma.getHeight(),
						ma.getDurationMs(), ma.getBlurhash(), ma.getThumbnailKey() != null,
						ma.getSizeBytes() == null ? null : ma.getSizeBytes().toString(),
						ma.getCreatedAt()))
				.collect(Collectors.toList());
		return new MessageDto(m.getId(), m.getConversationId(), senderId, m.getType().name().toLowerCase(), m.getBody(), media, m.getCreatedAt());
	}
}
