package io.github.james5635.chafgram.identity.service;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.PasskeyDto;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import io.github.james5635.chafgram.identity.User;

/**
 * Passkey management for the signed-in member (FR-1.6). Operates on the
 * framework-managed {@code user_entities}/{@code user_credentials} tables via
 * the Spring Security WebAuthn repositories plus targeted JDBC label updates —
 * no JPA entities, no DDL changes.
 */
@Service
public class PasskeyService {

	private final JdbcPublicKeyCredentialUserEntityRepository entities;
	private final JdbcUserCredentialRepository credentials;
	private final JdbcTemplate jdbc;

	public PasskeyService(JdbcPublicKeyCredentialUserEntityRepository entities,
			JdbcUserCredentialRepository credentials, JdbcTemplate jdbc) {
		this.entities = entities;
		this.credentials = credentials;
		this.jdbc = jdbc;
	}

	/** All passkeys of the current member, oldest first (deterministic). */
	public List<PasskeyDto> list(User actor) {
		try {
			PublicKeyCredentialUserEntity entity = entityOf(actor);
			return credentials.findByUserId(entity.getId()).stream()
					.sorted(Comparator.comparing(
							record -> record.getCreated() != null ? record.getCreated() : Instant.EPOCH,
							Comparator.naturalOrder()))
					.map(PasskeyService::toDto)
					.toList();
		}
		catch (Exception e) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "list failed: " + e.getMessage());
		}
	}

	/** Renames one of the member's passkeys; scoped to the caller's entity. */
	public PasskeyDto rename(User actor, String credentialId, String label) {
		PublicKeyCredentialUserEntity entity = entityOf(actor);
		String trimmed = label == null ? "" : label.trim();
		if (trimmed.isEmpty() || trimmed.length() > 200) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Label is required (max 200 chars)");
		}
		int updated = jdbc.update(
				"UPDATE user_credentials SET label = ? WHERE credential_id = ? AND user_entity_user_id = ?",
				trimmed, credentialId, entity.getId().toBase64UrlString());
		if (updated == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Passkey not found");
		}
		CredentialRecord record = credentials.findByCredentialId(Bytes.fromBase64(credentialId));
		return toDto(record);
	}

	/** Revokes one of the member's passkeys; scoped to the caller's entity. */
	public void revoke(User actor, String credentialId) {
		PublicKeyCredentialUserEntity entity = entityOf(actor);
		CredentialRecord record = credentials.findByCredentialId(Bytes.fromBase64(credentialId));
		if (record == null || !record.getUserEntityUserId().equals(entity.getId())) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Passkey not found");
		}
		credentials.delete(record.getCredentialId());
	}

	private PublicKeyCredentialUserEntity entityOf(User actor) {
		PublicKeyCredentialUserEntity entity = entities.findByUsername(actor.getUsername());
		if (entity == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "No passkeys for this account");
		}
		return entity;
	}

	private static PasskeyDto toDto(CredentialRecord record) {
		return new PasskeyDto(
				record.getCredentialId().toBase64UrlString(),
				record.getLabel(),
				record.getCreated(),
				record.getLastUsed());
	}
}
