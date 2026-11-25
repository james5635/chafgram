package io.github.james5635.chafgram.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import io.github.james5635.chafgram.social.SavedPost;
import io.github.james5635.chafgram.social.SavedPostId;

public interface SavedPostRepository extends JpaRepository<SavedPost, SavedPostId> {

	@Query("select s.postId from SavedPost s where s.userId = :userId order by s.postId desc")
	List<Long> findPostIdsByUser(@Param("userId") Long userId);

	@Query("select case when count(s) > 0 then true else false end from SavedPost s where s.userId = :userId and s.postId = :postId")
	boolean isSaved(@Param("userId") Long userId, @Param("postId") Long postId);
}
