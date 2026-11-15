package io.github.james5635.chafgram.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

	@Query("select m from ConversationMember m where m.conversationId = :conversationId and m.leftAt is null")
	List<ConversationMember> findActiveByConversation(@Param("conversationId") Long conversationId);

	@Query("select m from ConversationMember m where m.conversationId = :conversationId and m.userId = :userId")
	Optional<ConversationMember> findByConversationAndUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
