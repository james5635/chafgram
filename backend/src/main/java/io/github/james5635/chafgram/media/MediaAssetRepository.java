package io.github.james5635.chafgram.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/** Media asset repository with pipeline update hooks. */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

	@Modifying
	@Query("UPDATE MediaAsset a SET a.width = :width, a.height = :height, a.durationMs = :durationMs WHERE a.id = :id")
	void updateMeta(@Param("id") Long id, @Param("width") Integer width, @Param("height") Integer height, @Param("durationMs") Integer durationMs);

	@Modifying
	@Query("UPDATE MediaAsset a SET a.sizeBytes = :size WHERE a.id = :id")
	void updateSize(@Param("id") Long id, @Param("size") Long size);

	@Modifying
	@Query("UPDATE MediaAsset a SET a.status = 'ready', a.blurhash = :blurhash WHERE a.id = :id")
	void markReady(@Param("id") Long id, @Param("blurhash") String blurhash);
}
