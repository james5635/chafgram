package io.github.james5635.chafgram.social;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("select p from Post p where p.id = :id and p.deletedAt is null")
	Optional<Post> findVisibleById(@Param("id") Long id);

	@Query("select p from Post p where p.authorId in :authorIds and p.deletedAt is null and p.id < :cursor order by p.id desc")
	List<Post> findFeed(@Param("authorIds") List<Long> authorIds, @Param("cursor") long cursor, Pageable pageable);

	@Query("select p from Post p where p.deletedAt is null and p.id < :cursor order by p.id desc")
	List<Post> findDiscover(@Param("cursor") long cursor, Pageable pageable);

	@Query(value = """
		select p.* from posts p
		join post_media pm on pm.post_id = p.id
		join media_assets ma on ma.id = pm.media_id
		where p.deleted_at is null
		  and p.type = 'VIDEO'
		  and ma.kind = 'VIDEO'
		  and ma.status = 'READY'
		  and ma.duration_ms <= 60000
		  and p.id < :cursor
		order by p.id desc
		""", nativeQuery = true)
	List<Post> findWatch(@Param("cursor") long cursor, Pageable pageable);

	@Query("select p from Post p where p.authorId = :authorId and p.deletedAt is null order by p.id desc")
	List<Post> findByAuthor(@Param("authorId") Long authorId, Pageable pageable);

	@Query(value = "select id from posts where deleted_at is null and match(body) against(:q in boolean mode) order by id desc", nativeQuery = true)
	List<Long> searchIds(@Param("q") String q, Pageable pageable);

	@Query(value = """
		select id from posts
		where deleted_at is null
		  and (:kind is null or type = :kind)
		  and match(body) against(:q in boolean mode)
		order by id desc
		""", nativeQuery = true)
	List<Long> searchIdsWithKind(@Param("q") String q, @Param("kind") String kind, Pageable pageable);
}
