package io.github.james5635.chafgram.media;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** One image or video owned by a member. Transcoding is a worker concern (future). */
@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
public class MediaAsset {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "owner_user_id", nullable = false)
	private Long ownerUserId;

	@Convert(converter = MediaKindConverter.class)
	@Column(nullable = false, length = 5)
	private MediaKind kind = MediaKind.IMAGE;

	@Convert(converter = MediaStatusConverter.class)
	@Column(nullable = false, length = 10)
	private MediaStatus status = MediaStatus.PROCESSING;

	@Column(name = "storage_key", nullable = false, length = 512)
	private String storageKey;

	@Column(name = "mime_type", nullable = false, length = 127)
	private String mimeType;

	private Integer width;
	private Integer height;

	@Column(name = "duration_ms")
	private Integer durationMs;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@Column(length = 64)
	private String blurhash;

	/** Key of the generated thumbnail (videos); null when there is none. */
	@Column(name = "thumbnail_key", length = 512)
	private String thumbnailKey;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
