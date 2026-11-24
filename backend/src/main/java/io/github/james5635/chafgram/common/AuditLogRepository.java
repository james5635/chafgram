package io.github.james5635.chafgram.common;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	/**
	 * Staff-facing audit-log search (FR-10.2). Every filter is optional; a
	 * {@code null} argument disables that filter.
	 */
	@Query("select a from AuditLog a "
			+ "where (:actorId is null or a.actorId = :actorId) "
			+ "and (:action is null or a.action = :action) "
			+ "and (:targetType is null or a.targetType = :targetType) "
			+ "and (:from is null or a.createdAt >= :from) "
			+ "and (:to is null or a.createdAt <= :to) "
			+ "order by a.id desc")
	List<AuditLog> findByCriteria(@Param("actorId") Long actorId,
			@Param("action") String action,
			@Param("targetType") String targetType,
			@Param("from") Instant from,
			@Param("to") Instant to,
			Pageable pageable);

	@Query("select a from AuditLog a order by a.id desc")
	List<AuditLog> findRecent(Pageable pageable);
}
