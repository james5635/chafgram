package io.github.james5635.chafgram.identity.dto;

import java.time.Instant;

/** One registered passkey of the current member (FR-1.6). */
public record PasskeyDto(
		String credentialId,
		String label,
		Instant createdAt,
		Instant lastUsedAt) {
}
