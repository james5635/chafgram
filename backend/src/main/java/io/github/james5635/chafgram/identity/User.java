package io.github.james5635.chafgram.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** A member of the workspace. Mapped to the `users` table. */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String username;

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(length = 160)
	private String bio;

	@Column(name = "avatar_media_id")
	private Long avatarMediaId;

	@Convert(converter = RoleConverter.class)
	@Column(nullable = false, length = 9)
	private Role role = Role.MEMBER;

	@Convert(converter = UserStatusConverter.class)
	@Column(nullable = false, length = 11)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "last_seen_at")
	private Instant lastSeenAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public Long getAvatarMediaId() {
		return avatarMediaId;
	}

	public void setAvatarMediaId(Long avatarMediaId) {
		this.avatarMediaId = avatarMediaId;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
