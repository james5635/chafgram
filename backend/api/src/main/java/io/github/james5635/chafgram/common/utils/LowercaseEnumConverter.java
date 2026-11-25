package io.github.james5635.chafgram.common.utils;

import jakarta.persistence.AttributeConverter;

/**
 * Base converter for enums stored as lowercase in MySQL ENUMs. Instantiate per
 * enum via a package-private nested class in the enum's file.
 */
public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

	private final Class<E> enumType;

	protected LowercaseEnumConverter(Class<E> enumType) {
		this.enumType = enumType;
	}

	@Override
	public String convertToDatabaseColumn(E value) {
		return value == null ? null : value.name().toLowerCase();
	}

	@Override
	public E convertToEntityAttribute(String value) {
		if (value == null) {
			return null;
		}
		try {
			return Enum.valueOf(enumType, value.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException("Unknown " + enumType.getSimpleName() + " in database: " + value, e);
		}
	}
}
