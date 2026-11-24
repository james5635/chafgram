package io.github.james5635.chafgram.social;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query("select c from Comment c where c.id = :id and c.deletedAt is null")
	Optional<Comment> findVisibleById(@Param("id") Long id);

	@Query("select c from Comment c where c.postId = :postId order by c.id asc")
	List<Comment> findByPost(@Param("postId") Long postId, Pageable pageable);

	@Query("select count(c) from Comment c where c.postId = :postId and c.deletedAt is null")
	long countVisibleByPost(@Param("postId") Long postId);

	@Query("select count(c) from Comment c where c.id = :id and c.deletedAt is null")
	long countVisibleComment(@Param("id") Long id);

	/** Bulk visibility check — one query for a whole page of notifications. */
	@Query("select c.id from Comment c where c.id in :ids and c.deletedAt is null")
	List<Long> findVisibleIds(@Param("ids") List<Long> ids);
}
