package io.github.james5635.chafgram.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

	@Query("select m from Message m where m.conversationId = :conversationId and m.deletedAt is null and m.id < :cursor order by m.id desc")
	List<Message> findPage(@Param("conversationId") Long conversationId, @Param("cursor") long cursor, Pageable pageable);

	@Query("select m from Message m where m.conversationId = :conversationId and m.deletedAt is null order by m.id desc")
	Optional<Message> findLatestMessage(@Param("conversationId") Long conversationId);

	@Query("select count(m) from Message m where m.conversationId = :conversationId and m.deletedAt is null and m.id > :afterId")
	long countUnread(@Param("conversationId") Long conversationId, @Param("afterId") long afterId);

	@Query("select count(m) from Message m where m.id = :id and m.deletedAt is null")
	long countVisibleMessage(@Param("id") Long id);

	/** Bulk visibility check — one query for a whole page of notifications. */
	@Query("select m.id from Message m where m.id in :ids and m.deletedAt is null")
	List<Long> findVisibleIds(@Param("ids") List<Long> ids);
}
