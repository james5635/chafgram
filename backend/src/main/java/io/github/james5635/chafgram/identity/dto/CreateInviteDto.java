package io.github.james5635.chafgram.identity.dto;

import java.time.Instant;

/** Create-invite response: the only place the raw token is exposed. */
public record CreateInviteDto(
		Long id,
		String email,
		String role,
		String token,
		String acceptPath,
		Instant expiresAt) {

	public static CreateInviteDto from(CreateInviteResponse response) {
		return new CreateInviteDto(
				response.id(), response.email(), response.role(),
				response.token(), response.acceptPath(), response.expiresAt());
	}
}
