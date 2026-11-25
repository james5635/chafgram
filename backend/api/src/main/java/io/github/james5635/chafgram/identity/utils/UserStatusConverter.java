package io.github.james5635.chafgram.identity.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import io.github.james5635.chafgram.identity.UserStatus;

/** Maps {@link UserStatus} constants to the lowercase enum values stored in MySQL. */
@Converter
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

	@Override
	public String convertToDatabaseColumn(UserStatus status) {
		return status == null ? null : status.name().toLowerCase();
	}

	@Override
	public UserStatus convertToEntityAttribute(String value) {
		if (value == null) {
			return null;
		}
		try {
			return UserStatus.valueOf(value.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException("Unknown user status in database: " + value, e);
		}
	}
}
