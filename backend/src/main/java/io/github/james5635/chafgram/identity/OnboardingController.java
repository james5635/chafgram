package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.SessionAuthenticator;
import io.github.james5635.chafgram.identity.dto.AcceptInviteRequest;
import io.github.james5635.chafgram.identity.dto.BootstrapRequest;
import io.github.james5635.chafgram.identity.dto.MemberResponse;
import io.github.james5635.chafgram.identity.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * One-time first-run setup (`POST /api/v1/bootstrap`, valid only while the
 * workspace has no members) and self-serve registration (`POST /api/v1/register`,
 * valid only when signup_mode is `open`). Both create the account and
 * immediately authenticate the caller.
 */
@RestController
public class OnboardingController {

	private final MemberService memberService;
	private final SessionAuthenticator sessionAuthenticator;

	private final InviteService invites;

	public OnboardingController(MemberService memberService, InviteService invites,
			SessionAuthenticator sessionAuthenticator) {
		this.memberService = memberService;
		this.invites = invites;
		this.sessionAuthenticator = sessionAuthenticator;
	}

	@PostMapping("/api/v1/bootstrap")
	@ResponseStatus(HttpStatus.CREATED)
	public MemberResponse bootstrap(@RequestBody BootstrapRequest request,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		User user = memberService.bootstrap(
				request.username(), request.displayName(), request.email(), request.password());
		authenticate(user, servletRequest, servletResponse);
		return MemberService.toResponse(user);
	}

	@PostMapping("/api/v1/invites/accept")
	@ResponseStatus(HttpStatus.CREATED)
	public MemberResponse accept(@RequestBody AcceptInviteRequest request,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		User user = invites.accept(
				request.token(),
				request.username(),
				request.displayName(),
				request.email(),
				request.password());
		List<SimpleGrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
		sessionAuthenticator.authenticate(user.getUsername(), authorities, servletRequest, servletResponse);
		return MemberService.toResponse(user);
	}

	@PostMapping("/api/v1/register")
	@ResponseStatus(HttpStatus.CREATED)
	public MemberResponse register(@RequestBody RegisterRequest request,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
		User user = memberService.registerOpen(
				request.username(), request.displayName(), request.email(), request.password());
		authenticate(user, servletRequest, servletResponse);
		return MemberService.toResponse(user);
	}

	private void authenticate(User user, HttpServletRequest request, HttpServletResponse response) {
		List<SimpleGrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
		sessionAuthenticator.authenticate(user.getUsername(), authorities, request, response);
	}
}
