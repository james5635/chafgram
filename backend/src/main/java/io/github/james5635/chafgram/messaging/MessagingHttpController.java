package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.MemberAccess;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.messaging.dto.MessageDto;
import io.github.james5635.chafgram.media.dto.MediaAssetDto;
import io.github.james5635.chafgram.media.MediaAssetRepository;
import io.github.james5635.chafgram.messaging.MessageMediaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * REST endpoint for sending messages with media (multipart upload).
 * GraphQL mutations are used for text-only messages; this endpoint handles
 * multipart uploads with optional media attachments.
 */
@RestController
public class MessagingHttpController {

	private final MessagingService messaging;
	private final MemberAccess access;
	private final MediaAssetRepository mediaAssets;
	private final MessageMediaRepository messageMedia;

	public MessagingHttpController(MessagingService messaging, MemberAccess access,
			MediaAssetRepository mediaAssets, MessageMediaRepository messageMedia) {
		this.messaging = messaging;
		this.access = access;
		this.mediaAssets = mediaAssets;
		this.messageMedia = messageMedia;
	}

	@PostMapping(value = "/api/v1/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> send(
			@RequestParam(required = false) String body,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Long conversationId,
			@RequestParam(required = false, name = "mediaIds") List<Long> mediaIdsParam,
			@RequestParam(required = false) List<MultipartFile> file) {

		User actor = access.requireWriteable(currentUsername());

		List<Long> mediaIds = new ArrayList<>();
		if (mediaIdsParam != null) {
			mediaIds.addAll(mediaIdsParam);
		}
		if (file != null) {
			for (MultipartFile f : file) {
				if (!f.isEmpty()) {
					var asset = messaging.uploadMediaForMessage(actor, f);
					mediaIds.add(asset.getId());
				}
			}
		}

		if (userId != null) {
			var msg = messaging.send(actor, userId, body, mediaIds);
			return ResponseEntity.ok(toDto(msg));
		} else if (conversationId != null) {
			var msg = messaging.sendToConversation(actor, conversationId, body, mediaIds);
			return ResponseEntity.ok(toDto(msg));
		} else {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Either userId or conversationId is required");
		}
	}

	private String currentUsername() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}

	private MessageDto toDto(Message m) {
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
}