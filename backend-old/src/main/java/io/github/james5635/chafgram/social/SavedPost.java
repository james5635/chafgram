package io.github.james5635.chafgram.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "saved_posts")
@IdClass(SavedPostId.class)
@Getter
@Setter
@NoArgsConstructor
public class SavedPost {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Id
	@Column(name = "post_id")
	private Long postId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
