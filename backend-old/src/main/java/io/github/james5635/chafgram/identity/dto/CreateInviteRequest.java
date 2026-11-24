package io.github.james5635.chafgram.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Admin creates an invite. `email` may be null → reusable join link. */
public record CreateInviteRequest(
		@Email(message = "must be a valid email address") @Size(max = 254) String email,
		String role) {
}
