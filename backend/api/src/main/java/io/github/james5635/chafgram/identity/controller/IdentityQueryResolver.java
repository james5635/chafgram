package io.github.james5635.chafgram.identity.controller;

import io.github.james5635.chafgram.identity.dto.InviteDto;
import io.github.james5635.chafgram.identity.dto.MemberDetailDto;
import io.github.james5635.chafgram.identity.dto.MemberResponse;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import io.github.james5635.chafgram.social.dto.MemberDto;
import io.github.james5635.chafgram.social.dto.Views;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.User;

@Controller
@PreAuthorize("isFullyAuthenticated()")
public class IdentityQueryResolver {

	private final MemberService memberService;
	private final InviteService inviteService;
	private final CurrentMember current;
	private final Views views;

	public IdentityQueryResolver(MemberService memberService, InviteService inviteService,
			CurrentMember current, Views views) {
		this.memberService = memberService;
		this.inviteService = inviteService;
		this.current = current;
		this.views = views;
	}

	/**
	 * Staff-only member directory (FR-10.1). {@code query} is a case-insensitive
	 * prefix match against username or display name.
	 */
	@QueryMapping
	@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
	public List<MemberDetailDto> members(@Argument Integer first, @Argument String query) {
		current.require();
		int limit = first == null ? 50 : Math.min(Math.max(first, 1), 200);
		return memberService.listMembers(query, limit).stream()
				.map(IdentityQueryResolver::toDetail)
				.toList();
	}

	@QueryMapping
	public MemberDto memberById(@Argument Long id) {
		User viewer = current.require();
		User target = memberService.findOrThrow(id);
		return views.member(target, viewer);
	}

	@QueryMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<InviteDto> invites() {
		return inviteService.list().stream()
				.map(i -> new InviteDto(i.id(), i.email(), i.role(), i.createdAt(), i.expiresAt(), i.acceptedAt()))
				.toList();
	}

	static MemberDetailDto toDetail(MemberResponse r) {
		return new MemberDetailDto(r.id(), r.username(), r.displayName(), r.email(), r.role(), r.status(),
				r.emailVerifiedAt(), r.createdAt());
	}
}
