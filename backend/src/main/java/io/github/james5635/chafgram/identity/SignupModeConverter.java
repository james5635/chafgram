package io.github.james5635.chafgram.identity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link SignupMode} constants to the lowercase enum values stored in MySQL. */
@Converter
public class SignupModeConverter implements AttributeConverter<SignupMode, String> {

	@Override
	public String convertToDatabaseColumn(SignupMode mode) {
		return mode == null ? null : mode.name().toLowerCase();
	}

	@Override
	public SignupMode convertToEntityAttribute(String value) {
		if (value == null) {
			return null;
		}
		try {
			return SignupMode.valueOf(value.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException("Unknown signup mode in database: " + value, e);
		}
	}
}
