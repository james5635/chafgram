package io.github.james5635.chafgram.social;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "comment_reactions")
@IdClass(CommentReactionId.class)
@Getter
@Setter
@NoArgsConstructor
public class CommentReaction {

	@Id
	@Column(name = "comment_id")
	private Long commentId;

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Convert(converter = ReactionTypeConverter.class)
	@Column(nullable = false, length = 5)
	private ReactionType type = ReactionType.LIKE;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
