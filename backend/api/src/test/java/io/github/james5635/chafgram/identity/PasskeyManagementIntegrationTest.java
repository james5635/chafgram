package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.anyOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.james5635.chafgram.identity.service.MemberService;

/**
 * FR-1.6: members can list, rename and revoke their own passkeys. Framework
 * tables ({@code user_entities}, {@code user_credentials}) are seeded directly.
 */
class PasskeyManagementIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	private final List<String> credentialIds = new ArrayList<>();
	private final List<String> entityIds = new ArrayList<>();

	@BeforeEach
	void setUp() {
		wipeSocial();
		memberService.bootstrap("alice", "Alice", "alice@example.com", "a-very-strong-password");
		entityIds.add(insertEntity("alice"));
		entityIds.add(insertEntity("bob"));
	}

	@AfterEach
	void cleanPasskeys() {
		for (String id : credentialIds) {
			jdbc.update("DELETE FROM user_credentials WHERE credential_id = ?", id);
		}
		for (String id : entityIds) {
			jdbc.update("DELETE FROM user_entities WHERE id = ?", id);
		}
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void myPasskeysReturnsOnlyOwnCredentials() throws Exception {
		String aliceKey = insertCredential(entityId("alice"), "Phone");
		String aliceKey2 = insertCredential(entityId("alice"), "Laptop");
		insertCredential(entityId("bob"), "Bobs key");
		credentialIds.addAll(List.of(aliceKey, aliceKey2));

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ myPasskeys { credentialId label } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.myPasskeys", hasSize(2)))
				.andExpect(jsonPath("$.data.myPasskeys[*].label", containsInAnyOrder("Phone", "Laptop")))
				.andExpect(jsonPath("$.data.myPasskeys[0].credentialId", anyOf(is(aliceKey), is(aliceKey2))));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void renamePasskeyChangesLabel() throws Exception {
		String key = insertCredential(entityId("alice"), "Phone");
		credentialIds.add(key);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { renamePasskey(credentialId: \\\"" + key
								+ "\\\", label: \\\"Work laptop\\\") { credentialId label } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.renamePasskey.label", is("Work laptop")))
				.andExpect(jsonPath("$.data.renamePasskey.credentialId", is(key)));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void revokePasskeyDeletesTheRow() throws Exception {
		String key = insertCredential(entityId("alice"), "Phone");
		credentialIds.add(key);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { revokePasskey(credentialId: \\\"" + key + "\\\") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.revokePasskey", is(true)));

		assertThatRowCount("user_credentials", key, 0);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void cannotRenameOrRevokeAnotherMembersPasskey() throws Exception {
		String bobsKey = insertCredential(entityId("bob"), "Bobs key");

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { renamePasskey(credentialId: \\\"" + bobsKey
								+ "\\\", label: \\\"Hijacked\\\") { label } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0]").exists())
				.andExpect(jsonPath("$.data").doesNotExist());

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { revokePasskey(credentialId: \\\"" + bobsKey + "\\\") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0]").exists())
				.andExpect(jsonPath("$.data").doesNotExist());

		// bob's row is untouched
		assertThatRowCount("user_credentials", bobsKey, 1);
	}

	private void assertThatRowCount(String table, String credentialId, int expected) {
		Integer count = jdbc.queryForObject(
				"SELECT COUNT(*) FROM " + table + " WHERE credential_id = ?", Integer.class, credentialId);
		Assertions.assertThat(count).isEqualTo(expected);
	}

	private String entityId(String name) {
		return entityIds.stream()
				.filter(id -> {
					String n = jdbc.queryForObject("SELECT name FROM user_entities WHERE id = ?", String.class, id);
					return name.equals(n);
				})
				.findFirst()
				.orElseThrow();
	}

	private String insertEntity(String name) {
		String id = new Bytes(("entity-" + name + "-" + System.nanoTime()).getBytes()).toBase64UrlString();
		jdbc.update("INSERT INTO user_entities (id, name, display_name) VALUES (?, ?, ?)",
				id, name, name);
		return id;
	}

	private String insertCredential(String entityId, String label) {
		String id = new Bytes(("credential-" + label + "-" + System.nanoTime()).getBytes()).toBase64UrlString();
		Instant now = Instant.now();
		jdbc.update("INSERT INTO user_credentials (credential_id, user_entity_user_id, public_key, "
						+ "signature_count, uv_initialized, backup_eligible, authenticator_transports, "
						+ "public_key_credential_type, backup_state, attestation_object, "
						+ "attestation_client_data_json, created, last_used, label) "
						+ "VALUES (?, ?, ?, 0, 0, 1, 'usb', 'public-key', 0, ?, ?, ?, ?, ?)",
				id, entityId, "public-key-bytes".getBytes(), new byte[0], new byte[0], now, now, label);
		return id;
	}
}
