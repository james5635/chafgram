package io.github.james5635.chafgram.config;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.service.MemberService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks the stateless contract that replaced double-submit CSRF: the backend
 * no longer issues {@code XSRF-TOKEN}, and mutating requests are authorized
 * solely by the bearer token (GraphQL keeps its HTTP 200 + {@code errors[]}
 * failure shape).
 */
class CsrfDoubleSubmitIntegrationTest extends AbstractIntegrationTest {

	private static final String QUERY = "{\"query\":\"{ me { username } }\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@BeforeEach
	void setUp() {
		wipeSocial();
		memberService.bootstrap("alice", "Alice", "alice@example.com", "a-very-strong-password");
	}

	@Test
	void mutatingRequestWithoutCsrfTokenReachesGraphqlAndIssuesNoCsrfCookie() throws Exception {
		mockMvc.perform(post("/graphql")
						.contentType("application/json")
						.content(QUERY))
				.andExpect(status().isOk())
				.andExpect(cookie().doesNotExist("XSRF-TOKEN"))
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("UNAUTHORIZED")));
	}

	@Test
	void staleCsrfCookieAndHeaderAreIgnored() throws Exception {
		// CSRF is disabled, so stale/mismatched tokens are irrelevant; the
		// request still reaches GraphQL and fails resolver authorization only.
		mockMvc.perform(post("/graphql")
						.cookie(new Cookie("XSRF-TOKEN", "stale"))
						.header("X-XSRF-TOKEN", "different")
						.contentType("application/json")
						.content(QUERY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("UNAUTHORIZED")));
	}

	@Test
	void publicEndpointIssuesNoSessionOrCsrfCookie() throws Exception {
		mockMvc.perform(post("/graphql")
						.contentType("application/json")
						.content("{\"query\":\"{ workspace { name } }\"}"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void authenticatedMutationSucceedsWithoutCsrfTokens() throws Exception {
		mockMvc.perform(post("/graphql")
						.contentType("application/json")
						.content("{\"query\":\"mutation { createPost(type: NOTE, body: \\\"stateless ok\\\") { id } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.createPost.id").exists())
				.andExpect(cookie().doesNotExist("XSRF-TOKEN"));
	}
}
