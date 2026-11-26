package io.github.james5635.chafgram.identity.controller;

import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.auth.TokenPair;
import io.github.james5635.chafgram.identity.auth.TokenService;
import io.github.james5635.chafgram.identity.dto.AcceptInviteRequest;
import io.github.james5635.chafgram.identity.dto.BootstrapRequest;
import io.github.james5635.chafgram.identity.dto.MemberResponse;
import io.github.james5635.chafgram.identity.dto.RegisterRequest;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * One-time first-run setup (`POST /api/v1/bootstrap`, valid only while the
 * workspace has no members), invite acceptance, and self-serve registration
 * (`POST /api/v1/register`, valid only when signup_mode is `open`).
 *
 * <p>Each flow creates the account and immediately issues the same token pair
 * as password login: identity fields stay at the top level of the response,
 * with the access token alongside them and the refresh token in a cookie.</p>
 */
@RestController
public class OnboardingController {

	private final MemberService memberService;
	private final InviteService invites;
	private final TokenService tokenService;
	private final boolean cookieSecure;

	public OnboardingController(MemberService memberService, InviteService invites,
			TokenService tokenService, Environment environment,
			@Value("${chafgram.cookie.secure:${CHAFGRAM_COOKIE_SECURE:}}") String cookieSecure) {
		this.memberService = memberService;
		this.invites = invites;
		this.tokenService = tokenService;
		this.cookieSecure = AuthController.resolveCookieSecure(cookieSecure, environment);
	}

	@PostMapping("/api/v1/bootstrap")
	@ResponseStatus(HttpStatus.CREATED)
	public OnboardingResponse bootstrap(@RequestBody BootstrapRequest request,
			HttpServletResponse response) {
		User user = memberService.bootstrap(
				request.username(), request.displayName(), request.email(), request.password());
		return issueTokens(user, response);
	}

	@PostMapping("/api/v1/invites/accept")
	@ResponseStatus(HttpStatus.CREATED)
	public OnboardingResponse accept(@RequestBody AcceptInviteRequest request,
			HttpServletResponse response) {
		User user = invites.accept(
				request.token(),
				request.username(),
				request.displayName(),
				request.email(),
				request.password());
		return issueTokens(user, response);
	}

	@PostMapping("/api/v1/register")
	@ResponseStatus(HttpStatus.CREATED)
	public OnboardingResponse register(@RequestBody RegisterRequest request,
			HttpServletResponse response) {
		User user = memberService.registerOpen(
				request.username(), request.displayName(), request.email(), request.password());
		return issueTokens(user, response);
	}

	private OnboardingResponse issueTokens(User user, HttpServletResponse response) {
		TokenPair tokens = tokenService.issue(user);
		AuthController.setRefreshCookie(response, tokens.refreshToken(), cookieSecure);
		return OnboardingResponse.of(MemberService.toResponse(user), tokens);
	}

	public record OnboardingResponse(
			Long id,
			String username,
			String displayName,
			String email,
			String role,
			String status,
			Instant emailVerifiedAt,
			Instant createdAt,
			String accessToken,
			String tokenType,
			long expiresIn) {

		static OnboardingResponse of(MemberResponse member, TokenPair tokens) {
			return new OnboardingResponse(
					member.id(), member.username(), member.displayName(), member.email(),
					member.role(), member.status(), member.emailVerifiedAt(), member.createdAt(),
					tokens.accessToken(), AuthController.TOKEN_TYPE, tokens.expiresIn());
		}
	}
}
