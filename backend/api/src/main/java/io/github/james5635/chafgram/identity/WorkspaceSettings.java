package io.github.james5635.chafgram.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import io.github.james5635.chafgram.identity.utils.SignupModeConverter;

/**
 * Singleton workspace configuration (id is always 1 — enforced at the DB level
 * by `CHECK (id = 1)`). One deployment serves exactly one workspace.
 */
@Entity
@Table(name = "workspace_settings")
public class WorkspaceSettings {

	public static final byte SINGLETON_ID = 1;

	@Id
	@Column(name = "id")
	private Byte id = SINGLETON_ID;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(nullable = false, length = 80)
	private String slug;

	@Column(name = "logo_media_id")
	private Long logoMediaId;

	@Column(name = "primary_color", length = 7)
	private String primaryColor;

	@Convert(converter = SignupModeConverter.class)
	@Column(name = "signup_mode", nullable = false, length = 11)
	private SignupMode signupMode = SignupMode.INVITE_ONLY;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Byte getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public Long getLogoMediaId() {
		return logoMediaId;
	}

	public void setLogoMediaId(Long logoMediaId) {
		this.logoMediaId = logoMediaId;
	}

	public String getPrimaryColor() {
		return primaryColor;
	}

	public void setPrimaryColor(String primaryColor) {
		this.primaryColor = primaryColor;
	}

	public SignupMode getSignupMode() {
		return signupMode;
	}

	public void setSignupMode(SignupMode signupMode) {
		this.signupMode = signupMode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
