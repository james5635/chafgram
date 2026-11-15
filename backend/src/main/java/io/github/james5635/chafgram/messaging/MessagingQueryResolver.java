package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.messaging.dto.ConversationDto;
import io.github.james5635.chafgram.messaging.dto.MessageDto;
import io.github.james5635.chafgram.social.CurrentMember;
import io.github.james5635.chafgram.social.dto.Views;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@PreAuthorize("isFullyAuthenticated()")
public class MessagingQueryResolver {

	private final MessagingService messaging;
	private final CurrentMember current;
	private final Views views;

	public MessagingQueryResolver(MessagingService messaging, CurrentMember current, Views views) {
		this.messaging = messaging;
		this.current = current;
		this.views = views;
	}

	@QueryMapping
	public List<ConversationDto> conversations(@Argument Integer first) {
		var actor = current.require();
		int limit = first == null ? 50 : Math.min(Math.max(first, 1), 100);
		return messaging.list(actor, limit).stream().map(MessagingQueryResolver::toDto).toList();
	}

	@QueryMapping
	public List<MessageDto> messages(@Argument Long conversationId, @Argument Integer first, @Argument String cursor) {
		var actor = current.require();
		int limit = first == null ? 50 : Math.min(Math.max(first, 1), 100);
		long afterId = views.decodeCursor(cursor);
		return messaging.messages(actor, conversationId, afterId, limit).stream().map(MessagingQueryResolver::toDto).toList();
	}

	@QueryMapping
	public long unreadMessageCount(@Argument Long conversationId) {
		return messaging.unreadCount(current.require(), conversationId);
	}

	private static ConversationDto toDto(Conversation c) {
		return new ConversationDto(c.getId(), c.getType().name().toLowerCase(), c.getTitle(), c.getUpdatedAt());
	}

	private static MessageDto toDto(Message m) {
		Long senderId = m.getSenderId() == null ? -1L : m.getSenderId();
		return new MessageDto(m.getId(), m.getConversationId(), senderId, m.getType().name().toLowerCase(), m.getBody(), m.getCreatedAt());
	}
}
