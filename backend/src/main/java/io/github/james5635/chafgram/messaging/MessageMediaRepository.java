package io.github.james5635.chafgram.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageMediaRepository extends JpaRepository<MessageMedia, MessageMediaId> {
}
