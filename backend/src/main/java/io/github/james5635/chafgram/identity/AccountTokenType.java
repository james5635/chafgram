package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

/**
 * The kinds of single-use account token stored in `account_tokens`.
 * <p>
 * The MySQL ENUM stores {@code verify_email} / {@code reset_password};
 * the converter maps to and from the lowercase spelling.
 */
public enum AccountTokenType {
	VERIFY_EMAIL,
	RESET_PASSWORD
}

@Converter
class AccountTokenTypeConverter extends LowercaseEnumConverter<AccountTokenType> {
	AccountTokenTypeConverter() {
		super(AccountTokenType.class);
	}
}
