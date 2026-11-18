package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.messaging.dto.ConversationDto;
import io.github.james5635.chafgram.messaging.dto.MessageDto;
import io.github.james5635.chafgram.messaging.MessageMediaRepository;
import io.github.james5635.chafgram.social.CurrentMember;
import io.github.james5635.chafgram.social.dto.Views;
import io.github.james5635.chafgram.media.dto.MediaAssetDto;
import io.github.james5635.chafgram.media.MediaAssetRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@PreAuthorize("isFullyAuthenticated()")
public class MessagingQueryResolver {

	private final MessagingService messaging;
	private final CurrentMember current;
	private final Views views;
	private final MediaAssetRepository mediaAssets;
	private final MessageMediaRepository messageMedia;

	public MessagingQueryResolver(MessagingService messaging, CurrentMember current, Views views,
			MediaAssetRepository mediaAssets, MessageMediaRepository messageMedia) {
		this.messaging = messaging;
		this.current = current;
		this.views = views;
		this.mediaAssets = mediaAssets;
		this.messageMedia = messageMedia;
	}

	@QueryMapping
	public List<ConversationDto> conversations(@Argument Integer first) {
		var actor = current.require();
		int limit = first == null ? 50 : Math.min(Math.max(first, 1), 100);
		return messaging.list(actor, limit).stream().map(MessagingQueryResolver::toDto).toList();
	}

	private static MessageDto toDto(Message m, MediaAssetRepository mediaAssets, MessageMediaRepository messageMedia) {
		Long senderId = m.getSenderId() == null ? -1L : m.getSenderId();
		List<MediaAssetDto> media = messageMedia.findByMessageId(m.getId()).stream()
				.map(mm -> mediaAssets.findById(mm.getMediaId()).orElse(null))
				.filter(Objects::nonNull)
				.map(ma -> new MediaAssetDto(ma.getId(), ma.getKind().name().toLowerCase(), ma.getStatus().name().toLowerCase(),
						ma.getOwnerUserId(), ma.getMimeType(), ma.getWidth(), ma.getHeight(),
						ma.getDurationMs(), ma.getBlurhash(), ma.getSizeBytes() == null ? null : ma.getSizeBytes().toString(),
						ma.getCreatedAt()))
				.collect(Collectors.toList());
		return new MessageDto(m.getId(), m.getConversationId(), senderId, m.getType().name().toLowerCase(), m.getBody(), media, m.getCreatedAt());
	}

	@QueryMapping
	public List<MessageDto> messages(@Argument Long conversationId, @Argument Integer first, @Argument String cursor) {
		var actor = current.require();
		int limit = first == null ? 50 : Math.min(Math.max(first, 1), 100);
		long afterId = views.decodeCursor(cursor);
		return messaging.messages(actor, conversationId, afterId, limit).stream()
				.map(m -> toDto(m, mediaAssets, messageMedia))
				.toList();
	}

	private static ConversationDto toDto(Conversation c) {
		return new ConversationDto(c.getId(), c.getType().name().toLowerCase(), c.getTitle(), c.getUpdatedAt());
	}
}
