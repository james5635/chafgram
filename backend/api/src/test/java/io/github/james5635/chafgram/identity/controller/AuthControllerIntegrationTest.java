package io.github.james5635.chafgram.identity.controller;

import com.jayway.jsonpath.JsonPath;
import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.config.JwtConfig;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.identity.dto.UpdateWorkspaceRequest;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.service.WorkspaceService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Token issuance on the REST auth endpoints and on every account-creation flow. */
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

	private static final String PASSWORD = "a-very-strong-password";
	private static final String REFRESH_COOKIE = "chafgram_refresh";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private WorkspaceService workspaceService;

	@Autowired
	private UserRepository users;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtConfig jwtConfig;

	@BeforeEach
	void clean() {
		jdbc.update("DELETE FROM refresh_tokens");
		wipeSocial();
		jdbc.update("UPDATE workspace_settings SET signup_mode = 'invite_only' WHERE id = 1");
	}

	@Test
	void loginAcceptsUsernameOrEmailAndSetsHttpOnlyRefreshCookie() throws Exception {
		User alice = memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);

		for (String login : List.of("alice", "alice@example.com", "  ALICE@EXAMPLE.COM  ")) {
			MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
							.with(user("browser")).with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"username\":\"" + login + "\",\"password\":\"" + PASSWORD + "\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.accessToken", not(emptyString())))
					.andExpect(jsonPath("$.tokenType", is("Bearer")))
					.andExpect(jsonPath("$.expiresIn", is(900)))
					.andExpect(jsonPath("$.refreshToken").doesNotExist())
					.andReturn();

			Jwt access = jwtDecoder.decode(accessToken(result));
			assertThat(access.getSubject()).isEqualTo(String.valueOf(alice.getId()));
			assertRefreshCookie(result, false);
			assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
					.noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
		}
	}

	@Test
	void loginRejectsBadCredentialsAndUnknownAccounts() throws Exception {
		memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);

		for (String body : List.of(
				"{\"username\":\"alice\",\"password\":\"wrong-password\"}",
				"{\"username\":\"nobody\",\"password\":\"" + PASSWORD + "\"}")) {
			mockMvc.perform(post("/api/v1/auth/login")
							.with(user("browser")).with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content(body))
					.andExpect(status().isUnauthorized())
					.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
		}
	}

	@Test
	void refreshRotatesTheCookieAndTheOldCookieCannotBeReused() throws Exception {
		memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);
		MvcResult login = login("alice", PASSWORD);
		String firstCookie = refreshCookie(login);
		String firstAccess = accessToken(login);

		MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
						.with(user("browser")).with(csrf())
						.cookie(new Cookie(REFRESH_COOKIE, firstCookie)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andReturn();

		String secondCookie = refreshCookie(refreshed);
		assertThat(secondCookie).isNotEqualTo(firstCookie);
		String nextAccess = accessToken(refreshed);
		assertThat(nextAccess).isNotEmpty();
		// JWTs minted in the same second carry the same second-resolution iat/exp,
		// so verify the fresh token by its claims rather than raw-string inequality.
		assertThat(jwtDecoder.decode(nextAccess).getSubject())
				.isEqualTo(jwtDecoder.decode(firstAccess).getSubject());
		assertRefreshCookie(refreshed, false);
		assertThat(refreshTokensCount()).isEqualTo(2);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.with(user("browser")).with(csrf())
						.cookie(new Cookie(REFRESH_COOKIE, firstCookie)))
				.andExpect(status().isUnauthorized());

		// The revoked ancestor must not have forked a second live chain.
		assertThat(refreshTokensCount()).isEqualTo(2);
	}

	@Test
	void logoutRequiresBearerRevokesTheCookieAndClearsIt() throws Exception {
		memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);
		MvcResult login = login("alice", PASSWORD);
		String rawCookie = refreshCookie(login);

		mockMvc.perform(post("/api/v1/auth/logout")
						.with(user("browser")).with(csrf())
						.cookie(new Cookie(REFRESH_COOKIE, rawCookie)))
				.andExpect(status().isUnauthorized());

		// Without a bearer nothing was revoked: the cookie still refreshes.
		mockMvc.perform(post("/api/v1/auth/refresh")
						.with(user("browser")).with(csrf())
						.cookie(new Cookie(REFRESH_COOKIE, rawCookie)))
				.andExpect(status().isOk());

		MvcResult current = login("alice", PASSWORD);
		String currentCookie = refreshCookie(current);
		MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout")
						.with(user("browser")).with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(current))
						.cookie(new Cookie(REFRESH_COOKIE, currentCookie)))
				.andExpect(status().isNoContent())
				.andReturn();
		assertThat(logout.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
				.anyMatch(cookie -> cookie.startsWith(REFRESH_COOKIE + "=")
						&& cookie.contains("Max-Age=0") && cookie.contains("HttpOnly")
						&& cookie.contains("SameSite=Strict") && cookie.contains("Path=/"));

		mockMvc.perform(post("/api/v1/auth/refresh")
						.with(user("browser")).with(csrf())
						.cookie(new Cookie(REFRESH_COOKIE, currentCookie)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void bootstrapIssuesTokenPairAndRefreshCookieWithoutCreatingASession() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/bootstrap")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(accountJson("alice", "Alice", "alice@example.com")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username", is("alice")))
				.andExpect(jsonPath("$.role", is("admin")))
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.expiresIn", is(900)))
				.andReturn();

		assertThat(jwtDecoder.decode(accessToken(result)).getSubject())
				.isEqualTo(String.valueOf(users.findByUsername("alice").orElseThrow().getId()));
		assertRefreshCookie(result, false);
		assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
				.noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
	}

	@Test
	void registerIssuesTokenPairAndRefreshCookie() throws Exception {
		User alice = memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);
		workspaceService.update(alice.getUsername(),
				new UpdateWorkspaceRequest(null, null, null, "open", null));

		MvcResult result = mockMvc.perform(post("/api/v1/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(accountJson("bob", "Bob", "bob@example.com")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username", is("bob")))
				.andExpect(jsonPath("$.role", is("member")))
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andReturn();

		assertThat(jwtDecoder.decode(accessToken(result)).getSubject())
				.isEqualTo(String.valueOf(users.findByUsername("bob").orElseThrow().getId()));
		assertRefreshCookie(result, false);
	}

	@Test
	void inviteAcceptIssuesTokenPairAndRefreshCookie() throws Exception {
		User alice = memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);
		CreateInviteResponse invite = inviteService.create(null, null, alice);

		MvcResult result = mockMvc.perform(post("/api/v1/invites/accept")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + invite.token() + "\",\"username\":\"bob\","
								+ "\"displayName\":\"Bob\",\"email\":\"bob@example.com\","
								+ "\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username", is("bob")))
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.expiresIn", is(900)))
				.andReturn();

		assertThat(jwtDecoder.decode(accessToken(result)).getSubject())
				.isEqualTo(String.valueOf(users.findByUsername("bob").orElseThrow().getId()));
		assertRefreshCookie(result, false);
	}

	@Test
	void logoutAcceptsAnExpiredBearerAndRevokesTheLiveRefreshToken() throws Exception {
		User alice = memberService.bootstrap("alice", "Alice", "alice@example.com", PASSWORD);
		MvcResult login = login("alice", PASSWORD);
		String rawCookie = refreshCookie(login);
		Instant now = Instant.now();

		JwtClaimsSet expiredClaims = JwtClaimsSet.builder()
				.issuer(jwtConfig.issuer())
				.audience(List.of(jwtConfig.audience()))
				.subject(String.valueOf(alice.getId()))
				.issuedAt(now.minusSeconds(3600))
				.expiresAt(now.minusSeconds(60))
				.claim("roles", List.of("admin"))
				.build();
		String expiredBearer = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), expiredClaims)).getTokenValue();
		// The resource-server decoder rejects it; logout must still accept it.
		assertThatThrownBy(() -> jwtDecoder.decode(expiredBearer)).isInstanceOf(JwtException.class);

		mockMvc.perform(post("/api/v1/auth/logout")
						.with(user("browser")).with(csrf())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredBearer)
						.cookie(new Cookie(REFRESH_COOKIE, rawCookie)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.with(user("browser")).with(csrf())
						.cookie(new Cookie(REFRESH_COOKIE, rawCookie)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cookieSecureDefaultsOnOutsideDevAndTestAndFailsClosedOnTypos() {
		MockEnvironment production = new MockEnvironment();
		MockEnvironment testing = new MockEnvironment();
		testing.setActiveProfiles("test");

		assertThat(AuthController.resolveCookieSecure("", production)).isTrue();
		assertThat(AuthController.resolveCookieSecure("", testing)).isFalse();
		assertThat(AuthController.resolveCookieSecure("true", testing)).isTrue();
		assertThat(AuthController.resolveCookieSecure("false", production)).isFalse();
		assertThat(AuthController.resolveCookieSecure("yes", production)).isTrue();
	}

	@Test
	void refreshWithoutACookieIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.with(user("browser")).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	private MvcResult login(String username, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
						.with(user("browser")).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
	}

	private String accessToken(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.accessToken");
	}

	private String accountJson(String username, String displayName, String email) {
		return "{\"username\":\"" + username + "\",\"displayName\":\"" + displayName
				+ "\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
	}

	private String refreshCookie(MvcResult result) {
		return Arrays.stream(result.getResponse().getCookies())
				.filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElseThrow();
	}

	private void assertRefreshCookie(MvcResult result, boolean secure) {
		List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		String header = cookies.stream()
				.filter(cookie -> cookie.startsWith(REFRESH_COOKIE + "="))
				.findFirst()
				.orElseThrow();
		assertThat(header).contains("HttpOnly", "SameSite=Strict", "Path=/");
		if (secure) {
			assertThat(header).contains("Secure");
		}
		else {
			assertThat(header).doesNotContain("; Secure");
		}
		long count = cookies.stream().filter(cookie -> cookie.startsWith(REFRESH_COOKIE + "=")).count();
		assertThat(count).isEqualTo(1);
	}

	private long refreshTokensCount() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Long.class);
	}
}
