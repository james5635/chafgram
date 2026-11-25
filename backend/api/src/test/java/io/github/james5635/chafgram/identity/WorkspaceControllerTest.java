package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.dto.UpdateWorkspaceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import io.github.james5635.chafgram.identity.repository.InviteRepository;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.service.WorkspaceService;

/**
 * Workspace query is public (needed pre-login); everything else on GraphQL
 * requires an authenticated session.
 */
class WorkspaceControllerTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private WorkspaceService workspaceService;

	@Autowired
	private InviteRepository invites;

	@Autowired
	private UserRepository users;

	@BeforeEach
	void cleanAndBootstrap() {
		invites.deleteAll();
		users.deleteAll();
		memberService.bootstrap("alice", "Alice", "alice@example.com", "a-very-strong-password");
		workspaceService.update("alice", new UpdateWorkspaceRequest(null, null, null, "invite_only", null));
	}

	@Test
	void workspaceQueryIsPublic() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ workspace { name signupMode } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.workspace.name", is("Chafgram")))
				.andExpect(jsonPath("$.data.workspace.signupMode", is("invite_only")));
	}

	@Test
	void workspaceQueryRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/graphql")
						.contentType("application/json")
						.content("{\"query\":\"{ workspace { name } }\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void authenticatedWorkspaceUpdateWorks() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { updateWorkspace(name: \\\"Acme Corp\\\", slug: \\\"acme-corp\\\", primaryColor: \\\"#ff8800\\\") { name slug primaryColor } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.updateWorkspace.name", is("Acme Corp")))
				.andExpect(jsonPath("$.data.updateWorkspace.slug", is("acme-corp")))
				.andExpect(jsonPath("$.data.updateWorkspace.primaryColor", is("#ff8800")));

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ workspace { name slug } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.workspace.name", is("Acme Corp")));

		// restore defaults so other tests are unaffected
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { updateWorkspace(name: \\\"Chafgram\\\", slug: \\\"chafgram\\\") { name slug } }\"}"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void authenticatedWorkspaceUpdateWorksAsAdmin() throws Exception {
		// sanity check with an explicit authenticated user and CSRF token
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { updateWorkspace(primaryColor: \\\"#4fb8b2\\\") { primaryColor } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.updateWorkspace.primaryColor", is("#4fb8b2")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "MEMBER")
	void memberCannotUpdateWorkspace() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { updateWorkspace(name: \\\"Nope\\\") { name } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0]").exists())
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void anonymousCannotReadAuthenticatedFields() throws Exception {
		// Spring GraphQL answers authorization failures as HTTP 200 + errors[]
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ me { username } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0]").exists())
				.andExpect(jsonPath("$.data").doesNotExist());
	}
}
