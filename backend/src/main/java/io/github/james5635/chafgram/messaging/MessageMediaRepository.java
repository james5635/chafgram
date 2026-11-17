package io.github.james5635.chafgram.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageMediaRepository extends JpaRepository<MessageMedia, MessageMediaId> {

	List<MessageMedia> findByMessageId(Long messageId);
}
