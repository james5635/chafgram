package io.github.james5635.chafgram.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

	@Query("select b.blockedId from UserBlock b where b.blockerId = :id")
	List<Long> findBlockedIds(@Param("id") Long id);

	@Query("select case when count(b) > 0 then true else false end from UserBlock b where b.blockerId = :blockerId and b.blockedId = :blockedId")
	boolean isBlockedBy(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);
}
