package io.github.james5635.chafgram.identity.auth;

import io.github.james5635.chafgram.identity.User;
import jakarta.persistence.Column;
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

/**
 * A stored refresh-token record. {@code tokenHash} is the SHA-256 hex of the
 * opaque token the member holds — the raw token is never persisted.
 *
 * <p>{@code replacedBy} is set when rotation revokes the row; a row revoked
 * without it (logout or suspected theft) triggers user-wide revocation when
 * presented again.</p>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Mirrors {@link User#getId()}; a plain id keeps this table independent of JPA cascades. */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by")
	private Long replacedBy;
}
