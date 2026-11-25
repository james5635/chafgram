package io.github.james5635.chafgram.config;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.service.MemberService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks the double-submit CSRF contract that BFF clients rely on.
 *
 * <p>The backend issues the {@code XSRF-TOKEN} cookie (readable by JavaScript)
 * and expects the very same value in the {@code X-XSRF-TOKEN} header on every
 * mutating request. The token must be accepted verbatim — Spring Security's
 * default XOR-masked request handler only accepts a server-side masked value,
 * which a BFF can never obtain, so it is explicitly replaced with the plain
 * handler in {@link SecurityConfiguration}.</p>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfDoubleSubmitIntegrationTest extends AbstractIntegrationTest {

	private static final String TOKEN = "0f6b0f4a-3a52-4d2f-9a1c-6c1f0b5f2f11";

	private static final String OTHER_TOKEN = "9d3d1c77-2b0e-4f2a-8c55-1f0a7d9e4b32";

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
	void mutatingRequestWithoutCsrfTokenIsForbiddenAndIssuesTheCookie() throws Exception {
		mockMvc.perform(post("/graphql")
						.contentType("application/json")
						.content(QUERY))
				.andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
				.andExpect(status().isForbidden())
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@Test
	void mutatingRequestWithMatchingCookieAndHeaderPassesCsrf() throws Exception {
		// CSRF passed → the request reaches GraphQL, which answers an
		// authorization failure as HTTP 200 + errors[] (not a 403).
		mockMvc.perform(post("/graphql")
						.cookie(new Cookie("XSRF-TOKEN", TOKEN))
						.header("X-XSRF-TOKEN", TOKEN)
						.contentType("application/json")
						.content(QUERY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("UNAUTHORIZED")));
	}

	@Test
	void mutatingRequestWithMismatchedTokenIsForbidden() throws Exception {
		mockMvc.perform(post("/graphql")
						.cookie(new Cookie("XSRF-TOKEN", TOKEN))
						.header("X-XSRF-TOKEN", OTHER_TOKEN)
						.contentType("application/json")
						.content(QUERY))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void authenticatedMutationWithDoubleSubmitTokenSucceeds() throws Exception {
		mockMvc.perform(post("/graphql")
						.cookie(new Cookie("XSRF-TOKEN", TOKEN))
						.header("X-XSRF-TOKEN", TOKEN)
						.contentType("application/json")
						.content("{\"query\":\"mutation { createPost(type: NOTE, body: \\\"csrf ok\\\") { id } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.createPost.id").exists());
	}
}
