package io.github.james5635.chafgram.identity.controller;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.CreateInviteDto;
import io.github.james5635.chafgram.identity.dto.MemberDetailDto;
import io.github.james5635.chafgram.identity.dto.MemberResponse;
import io.github.james5635.chafgram.identity.dto.UpdateWorkspaceRequest;
import io.github.james5635.chafgram.identity.dto.WorkspaceDto;
import io.github.james5635.chafgram.identity.dto.WorkspaceResponse;
import io.github.james5635.chafgram.identity.dto.PasskeyDto;
import io.github.james5635.chafgram.social.dto.MemberDto;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import io.github.james5635.chafgram.identity.service.AccountFlowService;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.service.PasskeyService;
import io.github.james5635.chafgram.identity.service.WorkspaceService;
import io.github.james5635.chafgram.identity.utils.Validation;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserStatus;

/** Workspace, member lifecycle and invite mutations. Admin actions enforced per method. */
@Controller
@PreAuthorize("isFullyAuthenticated()")
public class IdentityMutationResolver {

	private final MemberService memberService;
	private final WorkspaceService workspaceService;
	private final InviteService invites;
	private final PasskeyService passkeyService;
	private final AccountFlowService accountFlows;
	private final MemberAccess access;
	private final CurrentMember current;

	public IdentityMutationResolver(MemberService memberService, WorkspaceService workspaceService,
			InviteService invites, PasskeyService passkeyService, AccountFlowService accountFlows,
			MemberAccess access, CurrentMember current) {
		this.memberService = memberService;
		this.workspaceService = workspaceService;
		this.invites = invites;
		this.passkeyService = passkeyService;
		this.accountFlows = accountFlows;
		this.access = access;
		this.current = current;
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public WorkspaceDto updateWorkspace(@Argument String name, @Argument String slug,
			@Argument String primaryColor, @Argument String signupMode, @Argument Long logoMediaId) {
		String username = current.require().getUsername();
		WorkspaceResponse updated = workspaceService.update(
				username, new UpdateWorkspaceRequest(name, slug, primaryColor, signupMode, logoMediaId));
		return new WorkspaceDto(updated.name(), updated.slug(), updated.primaryColor(),
				updated.signupMode(), updated.logoMediaId());
	}

	@MutationMapping
	public MemberDto updateProfile(@Argument String displayName, @Argument String bio,
			@Argument Long avatarMediaId) {
		String username = current.require().getUsername();
		return memberService.updateProfile(username, displayName, bio, avatarMediaId);
	}

	@MutationMapping
	public PasskeyDto renamePasskey(@Argument String credentialId, @Argument String label) {
		// Writeable-member check (FR-1.4, BR-5.2): suspended/deactivated
		// members cannot rename their passkeys either.
		User actor = access.requireWriteable(current.require().getUsername());
		return passkeyService.rename(actor, credentialId, label);
	}

	@MutationMapping
	public Boolean revokePasskey(@Argument String credentialId) {
		User actor = access.requireWriteable(current.require().getUsername());
		passkeyService.revoke(actor, credentialId);
		return true;
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public MemberDetailDto setMemberRole(@Argument Long id, @Argument String role) {
		String username = current.require().getUsername();
		return toDetail(memberService.updateRole(username, id, Validation.parseRole(role)));
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public MemberDetailDto suspendMember(@Argument Long id) {
		String username = current.require().getUsername();
		return toDetail(memberService.setStatus(username, id, UserStatus.SUSPENDED));
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public MemberDetailDto deactivateMember(@Argument Long id) {
		String username = current.require().getUsername();
		return toDetail(memberService.setStatus(username, id, UserStatus.DEACTIVATED));
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public MemberDetailDto reactivateMember(@Argument Long id) {
		String username = current.require().getUsername();
		return toDetail(memberService.setStatus(username, id, UserStatus.ACTIVE));
	}

	/**
	 * Mails a verification link to the signed-in member's own address (FR-1.7).
	 * A no-op once the address is verified.
	 */
	@MutationMapping
	public boolean requestEmailVerification() {
		accountFlows.requestEmailVerification(current.require());
		return true;
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public CreateInviteDto createInvite(@Argument String email, @Argument String role) {		String username = current.require().getUsername();
		User actor = access.requireWriteable(username);
		Role parsedRole = role == null || role.isBlank() ? Role.MEMBER : Validation.parseRole(role);
		return CreateInviteDto.from(invites.create(email, parsedRole, actor));
	}

	@MutationMapping
	@PreAuthorize("hasRole('ADMIN')")
	public boolean revokeInvite(@Argument Long id) {
		String username = current.require().getUsername();
		User actor = access.requireWriteable(username);
		invites.revoke(id, actor);
		return true;
	}

	private static MemberDetailDto toDetail(MemberResponse r) {
		return IdentityQueryResolver.toDetail(r);
	}
}
