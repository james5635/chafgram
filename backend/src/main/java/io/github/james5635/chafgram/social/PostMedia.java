package io.github.james5635.chafgram.social;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_media")
@IdClass(PostMediaId.class)
@Getter
@Setter
@NoArgsConstructor
public class PostMedia {

	@Id
	@Column(name = "post_id")
	private Long postId;

	@Id
	@Column(name = "media_id")
	private Long mediaId;

	@Column(nullable = false)
	private Short position;
}
