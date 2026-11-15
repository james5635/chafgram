package io.github.james5635.chafgram.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, ReactionId> {

	@Query("select r.type from PostReaction r where r.postId = :postId and r.userId = :userId")
	Optional<ReactionType> findTypeByPostAndUser(@Param("postId") Long postId, @Param("userId") Long userId);

	@Query("select count(r) from PostReaction r where r.postId = :postId")
	long countByPost(@Param("postId") Long postId);
}
