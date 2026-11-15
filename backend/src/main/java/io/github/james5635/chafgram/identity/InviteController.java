package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.common.SessionAuthenticator;
import io.github.james5635.chafgram.identity.dto.AcceptInviteRequest;
import io.github.james5635.chafgram.identity.dto.CreateInviteRequest;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.identity.dto.InviteResponse;
import io.github.james5635.chafgram.identity.dto.MemberResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Invite management (admin-only) and the public invite-claim endpoint.
 * Accepting an invite creates the account and authenticates the caller.
 */
@RestController
public class InviteController {

	private final InviteService invites;
	private final MemberAccess access;
	private final SessionAuthenticator sessionAuthenticator;

	public InviteController(InviteService invites, MemberAccess access,
			SessionAuthenticator sessionAuthenticator) {
		this.invites = invites;
		this.access = access;
		this.sessionAuthenticator = sessionAuthenticator;
	}

	@PostMapping("/api/v1/invites")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	public CreateInviteResponse create(@RequestBody CreateInviteRequest request) {
		User actor = access.requireWriteable(currentUsername());
		Role role = request.role() == null || request.role().isBlank()
				? Role.MEMBER
				: Validation.parseRole(request.role());
		return invites.create(request.email(), role, actor);
	}

	@GetMapping("/api/v1/invites")
	@PreAuthorize("hasRole('ADMIN')")
	public List<InviteResponse> list() {
		User actor = access.requireWriteable(currentUsername());
		return invites.list();
	}

	@DeleteMapping("/api/v1/invites/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	public void revoke(@PathVariable Long id) {
		User actor = access.requireWriteable(currentUsername());
		invites.revoke(id, actor);
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
		sessionAuthenticator.authenticate(
				user.getUsername(),
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
				servletRequest,
				servletResponse);
		return MemberService.toResponse(user);
	}

	private String currentUsername() {
		return SecurityContextHolder.getContext().getAuthentication().getName();
	}
}
