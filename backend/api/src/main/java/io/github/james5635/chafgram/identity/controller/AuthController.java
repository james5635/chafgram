package io.github.james5635.chafgram.identity.controller;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.auth.TokenPair;
import io.github.james5635.chafgram.identity.auth.TokenService;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.MemberUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Password login, refresh-token rotation, and logout.
 *
 * <p>The access token is returned in the JSON body; the opaque refresh token
 * only ever travels in an HTTP-only cookie, so browser JavaScript cannot read
 * it. Logout needs a valid bearer token as well as the cookie: clearing a
 * cookie alone is not proof that the caller owns the account.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	static final String REFRESH_COOKIE = "chafgram_refresh";
	static final String TOKEN_TYPE = "Bearer";
	private static final String BEARER_PREFIX = "Bearer ";

	private final MemberUserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final UserRepository users;
	private final TokenService tokenService;
	private final JwtDecoder jwtDecoder;
	private final boolean cookieSecure;

	public AuthController(MemberUserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
			UserRepository users, TokenService tokenService, JwtDecoder jwtDecoder,
			Environment environment,
			@Value("${chafgram.cookie.secure:${CHAFGRAM_COOKIE_SECURE:}}") String cookieSecure) {
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.users = users;
		this.tokenService = tokenService;
		this.jwtDecoder = jwtDecoder;
		this.cookieSecure = resolveCookieSecure(cookieSecure, environment);
	}

	@PostMapping("/login")
	public TokenResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
		UserDetails principal;
		try {
			// The lookup accepts username or email and rejects deactivated accounts.
			principal = userDetailsService.loadUserByUsername(request.username());
		}
		catch (UsernameNotFoundException e) {
			throw invalidCredentials();
		}
		if (request.password() == null
				|| !passwordEncoder.matches(request.password(), principal.getPassword())) {
			throw invalidCredentials();
		}
		User member = users.findByUsername(principal.getUsername()).orElseThrow(AuthController::invalidCredentials);
		TokenPair tokens = tokenService.issue(member);
		setRefreshCookie(response, tokens.refreshToken(), cookieSecure);
		return TokenResponse.from(tokens);
	}

	@PostMapping("/refresh")
	public TokenResponse refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletResponse response) {
		TokenPair tokens = tokenService.refresh(refreshToken);
		setRefreshCookie(response, tokens.refreshToken(), cookieSecure);
		return TokenResponse.from(tokens);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletResponse response) {
		requireBearer(authorization);
		tokenService.revoke(refreshToken);
		clearRefreshCookie(response, cookieSecure);
	}

	private void requireBearer(String authorization) {
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			throw invalidCredentials();
		}
		try {
			jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()).trim());
		}
		catch (JwtException | IllegalArgumentException e) {
			throw invalidCredentials();
		}
	}

	private static ApiException invalidCredentials() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
	}

	/** Shared with onboarding so every flow sets an identically scoped cookie. */
	static void setRefreshCookie(HttpServletResponse response, String token, boolean secure) {
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(token, secure, null).toString());
	}

	static void clearRefreshCookie(HttpServletResponse response, boolean secure) {
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", secure, 0).toString());
	}

	private static ResponseCookie refreshCookie(String value, boolean secure, Integer maxAge) {
		var builder = ResponseCookie.from(REFRESH_COOKIE, value == null ? "" : value)
				.httpOnly(true)
				.path("/")
				.sameSite("Strict")
				.secure(secure);
		if (maxAge != null) {
			builder.maxAge(maxAge);
		}
		return builder.build();
	}

	/** Explicit env/property wins; otherwise cookies are Secure except in dev/test. */
	static boolean resolveCookieSecure(String configured, Environment environment) {
		if (configured != null && !configured.isBlank()) {
			// Fail closed: a typo must never silently turn Secure off.
			return !"false".equalsIgnoreCase(configured.trim());
		}
		return !environment.acceptsProfiles(Profiles.of("dev", "test"));
	}

	public record LoginRequest(String username, String password) {
	}

	public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

		static TokenResponse from(TokenPair tokens) {
			return new TokenResponse(tokens.accessToken(), TOKEN_TYPE, tokens.expiresIn());
		}
	}
}
