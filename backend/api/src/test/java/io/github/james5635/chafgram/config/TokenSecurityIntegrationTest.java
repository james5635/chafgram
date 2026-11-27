package io.github.james5635.chafgram.config;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.auth.TokenService;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.MemberService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Locks the stateless resource-server cutover: bearer JWTs authenticate the
 * main chain, session/CSRF cookies are gone, and the pre-existing authorization
 * semantics (resolver {@code @PreAuthorize}, service-layer write block,
 * actuator's separate HTTP Basic chain) are unchanged.
 */
class TokenSecurityIntegrationTest extends AbstractIntegrationTest {

	private static final String PASSWORD = "a-very-strong-password";

	private static final String ME = "{ me { username role } }";

	private static final String ADMIN_ONLY = "{ invites { email } }";

	private static final String WRITE =
			"mutation { createPost(type: NOTE, body: \\\"suspended write\\\") { id } }";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private UserRepository users;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtConfig jwtConfig;

	@BeforeEach
	void setUp() {
		jdbc.update("DELETE FROM refresh_tokens");
		wipeSocial();
		memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);
	}

	// ── authentication at the HTTP boundary ─────────────────────────────────

	@Test
	void graphqlWithoutBearerIsUnauthorized() throws Exception {
		// /graphql is public at the HTTP boundary so the login page can query
		// workspace branding; the resolver's isFullyAuthenticated() denial is
		// still the GraphQL error contract (HTTP 200 + errors[]), never a 200.
		mockMvc.perform(graphql(null, ME))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("UNAUTHORIZED")))
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void validBearerAuthenticatesAnAuthorizedQuery() throws Exception {
		String bearer = tokenService.issue(alice()).accessToken();

		mockMvc.perform(graphql(bearer, ME))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors").doesNotExist())
				.andExpect(jsonPath("$.data.me.username", is("alice")))
				.andExpect(jsonPath("$.data.me.role", is("admin")));
	}

	@Test
	void invalidExpiredAndForeignBearerTokensAreRejected() throws Exception {
		for (String token : List.of("not-a-jwt", expiredToken(alice()), foreignToken(alice()))) {
			mockMvc.perform(graphql(token, ME))
					.andExpect(status().isUnauthorized());
		}
	}

	@Test
	void authEndpointsAreAnonymouslyReachableWithoutCsrf() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.refreshToken").doesNotExist());
	}

	@Test
	void expiredBearerForwardedByTheBffDoesNotBlockCookieRefresh() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		String refreshCookie = login.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
				.filter(cookie -> cookie.startsWith("chafgram_refresh="))
				.map(cookie -> cookie.substring("chafgram_refresh=".length(), cookie.indexOf(';')))
				.findFirst().orElseThrow();

		mockMvc.perform(post("/api/v1/auth/refresh")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken(alice()))
						.cookie(new Cookie("chafgram_refresh", refreshCookie)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.tokenType", is("Bearer")));
	}

	// ── authorization semantics stay unchanged ─────────────────────────────

	@Test
	void adminJwtCanRunAdminResolverButMemberJwtIsForbidden() throws Exception {
		insertMember("bob", "Bob", "member", "active");

		mockMvc.perform(graphql(tokenService.issue(alice()).accessToken(), ADMIN_ONLY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors").doesNotExist())
				.andExpect(jsonPath("$.data.invites").isArray());

		mockMvc.perform(graphql(tokenService.issue(user("bob")).accessToken(), ADMIN_ONLY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")))
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void suspendedMemberJwtCanReadButWriteIsForbiddenByTheServiceLayer() throws Exception {
		insertMember("maya", "Maya", "member", "suspended");
		String bearer = tokenService.issue(user("maya")).accessToken();

		mockMvc.perform(graphql(bearer, ME))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors").doesNotExist())
				.andExpect(jsonPath("$.data.me.username", is("maya")));

		mockMvc.perform(graphql(bearer, WRITE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")))
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	// ── actuator chain and cookie hygiene ──────────────────────────────────

	@Test
	void actuatorChainStillRequiresAdminBasicAndHealthStaysPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("UP")));

		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", containsString("Basic")));

		mockMvc.perform(get("/actuator/info").with(httpBasic("alice", PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.app.name", is("Chafgram")));

		mockMvc.perform(get("/actuator/info").with(httpBasic("alice", "wrong-password")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void noSessionOrCsrfCookiesAreEmitted() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		assertNoSessionCookies(login);

		MvcResult graphql = mockMvc.perform(graphql(tokenService.issue(alice()).accessToken(), ME))
				.andExpect(status().isOk())
				.andReturn();
		assertNoSessionCookies(graphql);
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	private MockHttpServletRequestBuilder graphql(String bearer, String query) {
		var request = post("/graphql")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"query\":\"" + query + "\"}");
		if (bearer != null) {
			request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
		}
		return request;
	}

	private User alice() {
		return user("alice");
	}

	private User user(String username) {
		return users.findByUsername(username).orElseThrow();
	}

	private void insertMember(String username, String displayName, String role, String status) {
		jdbc.update("INSERT INTO users (username, display_name, email, role, status) "
						+ "VALUES (?, ?, ?, ?, ?)",
				username, displayName, username + "@example.com", role, status);
	}

	private String expiredToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtConfig.issuer())
				.audience(List.of(jwtConfig.audience()))
				.subject(String.valueOf(user.getId()))
				.issuedAt(now.minusSeconds(3600))
				.expiresAt(now.minusSeconds(60))
				.claim("roles", List.of("admin"))
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	private String foreignToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(jwtConfig.issuer())
				.audience(List.of(jwtConfig.audience()))
				.subject(String.valueOf(user.getId()))
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900))
				.claim("roles", List.of("admin"))
				.build();
		JwtEncoder foreign = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(
				"foreign-dev-only-jwt-secret-for-tests-123456".getBytes(StandardCharsets.UTF_8),
				"HmacSHA256")).build();
		return foreign.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	private void assertNoSessionCookies(MvcResult result) {
		List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		assertThat(cookies).noneMatch(cookie -> cookie.contains("JSESSIONID"));
		assertThat(cookies).noneMatch(cookie -> cookie.contains("XSRF-TOKEN"));
	}
}
