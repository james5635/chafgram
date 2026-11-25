package io.github.james5635.chafgram.identity.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import io.github.james5635.chafgram.identity.Role;

/** Maps {@link Role} constants to the lowercase enum values stored in MySQL. */
@Converter
public class RoleConverter implements AttributeConverter<Role, String> {

	@Override
	public String convertToDatabaseColumn(Role role) {
		return role == null ? null : role.name().toLowerCase();
	}

	@Override
	public Role convertToEntityAttribute(String value) {
		if (value == null) {
			return null;
		}
		try {
			return Role.valueOf(value.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException("Unknown role in database: " + value, e);
		}
	}
}
