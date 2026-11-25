package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.identity.utils.RoleConverter;

/** Member roles. Persisted as lowercase via {@link RoleConverter}. */
public enum Role {
	MEMBER,
	MODERATOR,
	ADMIN
}
