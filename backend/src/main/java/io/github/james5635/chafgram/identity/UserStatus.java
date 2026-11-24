package io.github.james5635.chafgram.identity;

/** Member account lifecycle states. Persisted as lowercase via {@link UserStatusConverter}. */
public enum UserStatus {
	ACTIVE,
	SUSPENDED,
	DEACTIVATED
}
