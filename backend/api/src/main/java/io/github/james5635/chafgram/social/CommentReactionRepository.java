package io.github.james5635.chafgram.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, CommentReactionId> {

	@Query("select r.type from CommentReaction r where r.commentId = :commentId and r.userId = :userId")
	Optional<ReactionType> findTypeByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

	@Query("select count(r) from CommentReaction r where r.commentId = :commentId")
	long countByComment(@Param("commentId") Long commentId);
}
