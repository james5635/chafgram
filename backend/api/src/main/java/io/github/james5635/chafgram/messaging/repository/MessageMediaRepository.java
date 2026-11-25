package io.github.james5635.chafgram.messaging.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import io.github.james5635.chafgram.messaging.MessageMedia;
import io.github.james5635.chafgram.messaging.MessageMediaId;

public interface MessageMediaRepository extends JpaRepository<MessageMedia, MessageMediaId> {

	List<MessageMedia> findByMessageId(Long messageId);

	/** Attachments of a media asset — the inverse of {@link #findByMessageId(Long)}. */
	List<MessageMedia> findByMediaId(Long mediaId);
}
