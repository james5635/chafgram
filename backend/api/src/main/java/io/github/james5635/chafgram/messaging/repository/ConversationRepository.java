package io.github.james5635.chafgram.messaging.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import io.github.james5635.chafgram.messaging.Conversation;
import io.github.james5635.chafgram.messaging.ConversationMember;
import io.github.james5635.chafgram.messaging.ConversationType;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	@Query("select c from Conversation c where c.id = :id and (select count(m) from ConversationMember m where m.conversationId = c.id and m.userId = :userId and m.leftAt is null) > 0")
	Optional<Conversation> findAccessibleById(@Param("id") Long id, @Param("userId") Long userId);

	@Query("select c from Conversation c join ConversationMember m on m.conversationId = c.id where m.userId = :userId and m.leftAt is null order by c.updatedAt desc")
	List<Conversation> findByMember(@Param("userId") Long userId, Pageable pageable);

	@Query("select c from Conversation c where c.type = io.github.james5635.chafgram.messaging.ConversationType.DIRECT and (select count(m) from ConversationMember m where m.conversationId = c.id and m.userId = :a) = 1 and (select count(m) from ConversationMember m where m.conversationId = c.id and m.userId = :b) = 1 and (select count(m) from ConversationMember m where m.conversationId = c.id) = 2")
	Optional<Conversation> findDirectBetween(@Param("a") Long a, @Param("b") Long b);
}
