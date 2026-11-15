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
@Table(name = "user_blocks")
@IdClass(UserBlockId.class)
@Getter
@Setter
@NoArgsConstructor
public class UserBlock {

	@Id
	@Column(name = "blocker_id")
	private Long blockerId;

	@Id
	@Column(name = "blocked_id")
	private Long blockedId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
