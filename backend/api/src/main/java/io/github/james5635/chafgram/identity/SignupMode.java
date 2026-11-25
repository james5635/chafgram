package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.identity.utils.SignupModeConverter;

/** Workspace sign-up policy. Persisted as lowercase via {@link SignupModeConverter}. */
public enum SignupMode {
	INVITE_ONLY,
	OPEN
}
