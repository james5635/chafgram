package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.controller.IdentityMutationResolver;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.AccountFlowService;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.LastSeenService;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.service.WorkspaceService;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The writeable-member rule (FR-1.4, BR-5.2) still guards invite mutations
 * after the passkey mutations were removed: suspended/deactivated members
 * cannot change credentials or invite others.
 */
class IdentityMutationResolverTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CurrentMember current = mock(CurrentMember.class);
	private final InviteService invites = mock(InviteService.class);
	private final MemberAccess access = new MemberAccess(users, mock(LastSeenService.class));
	private final IdentityMutationResolver resolver = new IdentityMutationResolver(
			mock(MemberService.class), mock(WorkspaceService.class), invites,
			mock(AccountFlowService.class), access, current);

	@Test
	void createInviteRequiresAWriteableMember() {
		User alice = member(Role.ADMIN, UserStatus.SUSPENDED);
		when(current.require()).thenReturn(alice);
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

		assertThatThrownBy(() -> resolver.createInvite(null, null))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(invites, never()).create(any(), any(), any());
	}

	@Test
	void createInviteDelegatesToAnActiveMember() {
		User alice = member(Role.ADMIN, UserStatus.ACTIVE);
		when(current.require()).thenReturn(alice);
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
		CreateInviteResponse response = new CreateInviteResponse(
				1L, null, "member", "token", "/invites/token", Instant.now());
		when(invites.create(any(), eq(Role.MEMBER), eq(alice))).thenReturn(response);

		assertThat(resolver.createInvite(null, null).token()).isEqualTo("token");
		verify(invites).create(any(), eq(Role.MEMBER), eq(alice));
	}

	private User member(Role role, UserStatus status) {
		User user = new User();
		user.setUsername("alice");
		user.setRole(role);
		user.setStatus(status);
		return user;
	}
}
