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
@Table(name = "follows")
@IdClass(FollowId.class)
@Getter
@Setter
@NoArgsConstructor
public class Follow {

	@Id
	@Column(name = "follower_id")
	private Long followerId;

	@Id
	@Column(name = "followee_id")
	private Long followeeId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
