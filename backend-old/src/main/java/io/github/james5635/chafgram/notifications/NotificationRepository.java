package io.github.james5635.chafgram.notifications;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query("select n from Notification n where n.recipientId = :recipientId order by n.id desc")
	List<Notification> findByRecipient(@Param("recipientId") Long recipientId, Pageable pageable);

	@Query("select count(n) from Notification n where n.recipientId = :recipientId and n.readAt is null")
	long countUnread(@Param("recipientId") Long recipientId);

	@Query("select n from Notification n where n.recipientId = :recipientId and n.readAt is null")
	List<Notification> findUnreadByRecipient(@Param("recipientId") Long recipientId);

	@Query("select count(p) from Post p where p.id = :id and p.deletedAt is null")
	long countVisiblePost(@Param("id") Long id);

	@Query("select count(c) from Comment c where c.id = :id and c.deletedAt is null")
	long countVisibleComment(@Param("id") Long id);

	@Query("select count(m) from Message m where m.id = :id and m.deletedAt is null")
	long countVisibleMessage(@Param("id") Long id);
}
