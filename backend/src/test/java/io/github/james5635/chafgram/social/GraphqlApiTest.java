package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.MemberService;
import io.github.james5635.chafgram.identity.WorkspaceSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Smoke-tests the GraphQL surface end to end: schema loading, argument
 * binding, enum resolution, result mapping, and security/CSRF enforcement.
 */
@AutoConfigureMockMvc
class GraphqlApiTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private WorkspaceSettingsRepository settings;

	@BeforeEach
	void setUp() {
		wipeSocial();
		memberService.bootstrap("alice", "Alice", "alice@example.com", "a-very-strong-password");
	}

	@Test
	void graphqlRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/graphql")
						.contentType("application/json")
						.content("{\"query\":\"{ me { username } }\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void graphqlRequiresAuthentication() throws Exception {
		// Spring GraphQL answers authorization failures as HTTP 200 + errors[]
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ me { username } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0]").exists())
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void authenticatedMemberReadsOwnProfile() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ me { username role status } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.me.username", is("alice")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void createPostAndReadItBackViaFeed() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { createPost(type: NOTE, body: \\\"hello graphql\\\") { id type } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.createPost.type", is("NOTE")));

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ followingFeed(first: 20) { posts { body author { username } } } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.followingFeed.posts[0].body", is("hello graphql")))
				.andExpect(jsonPath("$.data.followingFeed.posts[0].author.username", is("alice")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void discoverFeedIsReadable() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ discoverFeed(first: 20) { posts { id } } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.discoverFeed.posts").isArray());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void updateProfileViaGraphQlIsReflectedInMe() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { updateProfile(displayName: \\\"Alice B\\\", bio: \\\"bio!\\\") { username bio } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.updateProfile.bio", is("bio!")));

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ me { bio } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.me.bio", is("bio!")));
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void nonAdminCannotUpdateWorkspace() throws Exception {
		jdbc.update("INSERT INTO users (username, display_name, email, role, status) "
				+ "VALUES ('bob', 'Bob', 'bob@example.com', 'member', 'active')");

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { updateWorkspace(name: \\\"Nope\\\") { name } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")))
				.andExpect(jsonPath("$.data").doesNotExist());
	}
}