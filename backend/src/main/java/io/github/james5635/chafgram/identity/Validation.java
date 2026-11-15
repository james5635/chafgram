package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.regex.Pattern;

/** Shared, deterministic field validation for the identity module. */
final class Validation {

	static final Pattern USERNAME = Pattern.compile("^[a-z0-9][a-z0-9-]{2,29}$");
	static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{1,78}[a-z0-9]$");
	static final Pattern PRIMARY_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
	private static final int MIN_PASSWORD_LENGTH = 12;

	private Validation() {
	}

	static String requireUsername(String username) {
		if (username == null || !USERNAME.matcher(username).matches()) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"Username must be 3–30 chars, lowercase letters/digits/hyphens, starting with a letter or digit");
		}
		return username;
	}

	static String requireDisplayName(String displayName) {
		if (displayName == null || displayName.isBlank() || displayName.length() > 80) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Display name is required (max 80 chars)");
		}
		return displayName.trim();
	}

	static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			return null;
		}
		String normalized = email.trim().toLowerCase();
		if (normalized.length() > 254 || !normalized.contains("@")) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Must be a valid email address");
		}
		return normalized;
	}

	/** Returns the normalized password, or null when absent (passkey-only account). */
	static String normalizePassword(String password) {
		if (password == null || password.isBlank()) {
			return null;
		}
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}
		return password;
	}

	static String requireSlug(String slug) {
		if (slug == null || !SLUG.matcher(slug).matches()) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"Slug must be 3–80 chars, lowercase letters/digits/hyphens, not starting or ending with a hyphen");
		}
		return slug;
	}

	static String optionalColor(String color) {
		if (color == null) {
			return null;
		}
		if (!PRIMARY_COLOR.matcher(color).matches()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Primary color must be a hex color like #4fb8b2");
		}
		return color;
	}

	static Role parseRole(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Role is required");
		}
		try {
			return Role.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST,
					"Role must be one of: member, moderator, admin");
		}
	}
}
