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

	/**
	 * Flips the asset to ready. Guarded on the current status: an admin
	 * moderation delete while the transcode was running must survive — the
	 * byte authorizer would otherwise serve the video again.
	 *
	 * @return the number of rows updated (0 when the asset is no longer
	 * processing, e.g. it was deleted)
	 */
	@Modifying
	@Transactional
	@Query("UPDATE MediaAsset a SET a.status = 'ready', a.blurhash = :blurhash WHERE a.id = :id AND a.status = 'processing'")
	int markReady(@Param("id") Long id, @Param("blurhash") String blurhash);

	/**
	 * Marks the asset failed. Guarded the same way: a tombstoned asset must
	 * not be flipped back to a servable state by the pipeline.
	 *
	 * @return the number of rows updated (0 when the asset is no longer processing)
	 */
	@Modifying
	@Transactional
	@Query("UPDATE MediaAsset a SET a.status = 'failed' WHERE a.id = :id AND a.status = 'processing'")
	int markFailed(@Param("id") Long id);
}
