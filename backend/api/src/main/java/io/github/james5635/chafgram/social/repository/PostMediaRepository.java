package io.github.james5635.chafgram.social.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import io.github.james5635.chafgram.social.PostMedia;
import io.github.james5635.chafgram.social.PostMediaId;

public interface PostMediaRepository extends JpaRepository<PostMedia, PostMediaId> {

	@Query("select pm from PostMedia pm where pm.mediaId = :mediaId")
	List<PostMedia> findByMediaId(@Param("mediaId") Long mediaId);

	@Query("select pm from PostMedia pm where pm.postId = :postId order by pm.position asc")
	List<PostMedia> findByPostIdOrderByPositionAsc(@Param("postId") Long postId);
}
