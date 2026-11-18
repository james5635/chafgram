package io.github.james5635.chafgram.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Media asset repository with pipeline update hooks. */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

	@Modifying
	@Transactional
	@Query("UPDATE MediaAsset a SET a.width = :width, a.height = :height, a.durationMs = :durationMs WHERE a.id = :id")
	void updateMeta(@Param("id") Long id, @Param("width") Integer width, @Param("height") Integer height, @Param("durationMs") Integer durationMs);

	@Modifying
	@Transactional
	@Query("UPDATE MediaAsset a SET a.sizeBytes = :size WHERE a.id = :id")
	void updateSize(@Param("id") Long id, @Param("size") Long size);

	/**
	 * Records the pipeline output: the transcode writes MP4 bytes, so the
	 * storage key and MIME type must follow the new container format.
	 */
	@Modifying
	@Transactional
	@Query("UPDATE MediaAsset a SET a.storageKey = :storageKey, a.mimeType = :mimeType, a.sizeBytes = :size WHERE a.id = :id")
	void updateOutput(@Param("id") Long id, @Param("storageKey") String storageKey,
			@Param("mimeType") String mimeType, @Param("size") Long size);

	@Modifying
	@Transactional
	@Query("UPDATE MediaAsset a SET a.status = 'ready', a.blurhash = :blurhash WHERE a.id = :id")
	void markReady(@Param("id") Long id, @Param("blurhash") String blurhash);
}
