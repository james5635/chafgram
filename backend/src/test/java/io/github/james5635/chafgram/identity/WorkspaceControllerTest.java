package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WorkspaceControllerTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteRepository invites;

	@Autowired
	private UserRepository users;

	@BeforeEach
	void cleanAndBootstrap() {
		invites.deleteAll();
		users.deleteAll();
		memberService.bootstrap("alice", "Alice", "alice@example.com", "a-very-strong-password");
	}

	@Test
	void getWorkspaceIsPublic() throws Exception {
		mockMvc.perform(get("/api/v1/workspace"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Chafgram"))
				.andExpect(jsonPath("$.signupMode").value("invite_only"));
	}

	@Test
	void patchRequiresCsrfToken() throws Exception {
		mockMvc.perform(patch("/api/v1/workspace")
						.contentType("application/json")
						.content("{\"name\":\"Acme Corp\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void adminCanUpdateWorkspace() throws Exception {
		mockMvc.perform(patch("/api/v1/workspace")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType("application/json")
						.content("{\"name\":\"Acme Corp\",\"slug\":\"acme-corp\",\"primaryColor\":\"#ff8800\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("Acme Corp")))
				.andExpect(jsonPath("$.slug", is("acme-corp")))
				.andExpect(jsonPath("$.primaryColor", is("#ff8800")));

		mockMvc.perform(get("/api/v1/workspace"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Acme Corp"));

		// restore defaults so other tests are unaffected
		mockMvc.perform(patch("/api/v1/workspace")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType("application/json")
						.content("{\"name\":\"Chafgram\",\"slug\":\"chafgram\",\"primaryColor\":null}"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void adminCannotSetInvalidColor() throws Exception {
		mockMvc.perform(patch("/api/v1/workspace")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType("application/json")
						.content("{\"primaryColor\":\"red\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "alice", roles = "MEMBER")
	void memberCannotUpdateWorkspace() throws Exception {
		mockMvc.perform(patch("/api/v1/workspace")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
						.contentType("application/json")
						.content("{\"name\":\"Nope\"}"))
				.andExpect(status().isForbidden());
	}
}
