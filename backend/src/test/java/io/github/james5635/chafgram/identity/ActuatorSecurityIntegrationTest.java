package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Actuator exposure (FR-10.4): health is a public probe; info and metrics are
 * admin-only.
 *
 * <p>Actuator sits behind its own filter chain with HTTP Basic, so anonymous
 * requests are answered with {@code 401} and a {@code WWW-Authenticate}
 * challenge (not a login redirect).</p>
 */
class ActuatorSecurityIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	private static final String ADMIN_PASSWORD = "a-very-strong-password";

	@BeforeEach
	void setUp() {
		wipeSocial();
		memberService.bootstrap("alice", "Alice", "alice@example.com", ADMIN_PASSWORD);
	}

	// ── health: public ──────────────────────────────────────────────────────

	@Test
	void anonymousHealthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("UP")));
	}

	@Test
	void healthDoesNotLeakDetails() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void adminCanReadHealth() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	// ── info and metrics: admin-only ────────────────────────────────────────

	@Test
	void anonymousInfoIsUnauthorized() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", containsString("Basic")));
	}

	@Test
	void anonymousMetricsIsUnauthorized() throws Exception {
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void plainMemberCannotReadMetrics() throws Exception {
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void plainMemberCannotReadInfo() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void adminCanReadInfo() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.app.name", is("Chafgram")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void adminCanListMetrics() throws Exception {
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.names").isArray());
	}

	@Test
	void adminCanAuthenticateWithHttpBasic() throws Exception {
		mockMvc.perform(get("/actuator/info").with(httpBasic("alice", ADMIN_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.app.name", is("Chafgram")));
	}

	@Test
	void wrongPasswordIsUnauthorized() throws Exception {
		mockMvc.perform(get("/actuator/info").with(httpBasic("alice", "not-the-password")))
				.andExpect(status().isUnauthorized());
	}
}
