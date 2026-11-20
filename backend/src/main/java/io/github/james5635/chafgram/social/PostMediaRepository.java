package io.github.james5635.chafgram.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, PostMediaId> {

	@Query("select pm from PostMedia pm where pm.mediaId = :mediaId")
	List<PostMedia> findByMediaId(@Param("mediaId") Long mediaId);
}
