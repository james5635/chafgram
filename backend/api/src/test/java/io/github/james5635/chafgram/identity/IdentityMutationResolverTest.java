package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.PasskeyDto;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.github.james5635.chafgram.identity.controller.IdentityMutationResolver;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.AccountFlowService;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.LastSeenService;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.service.PasskeyService;
import io.github.james5635.chafgram.identity.service.WorkspaceService;

/**
 * Passkey mutations (rename/revoke) must enforce the writeable-member rule
 * (FR-1.4, BR-5.2): suspended/deactivated members cannot change their
 * credentials, same as every other write path.
 */
class IdentityMutationResolverTest {

	private final UserRepository users = mock(UserRepository.class);
	private final PasskeyService passkeys = mock(PasskeyService.class);
	private final CurrentMember current = mock(CurrentMember.class);
	private final MemberAccess access = new MemberAccess(users, mock(LastSeenService.class));
	private final IdentityMutationResolver resolver = new IdentityMutationResolver(
			mock(MemberService.class), mock(WorkspaceService.class), mock(InviteService.class),
			passkeys, mock(AccountFlowService.class), access, current);

	@Test
	void renamePasskeyRequiresAWriteableMember() {
		User alice = member(Role.ADMIN, UserStatus.SUSPENDED);
		when(current.require()).thenReturn(alice);
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

		assertThatThrownBy(() -> resolver.renamePasskey("cred", "label"))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(passkeys, never()).rename(any(), anyString(), anyString());
	}

	@Test
	void revokePasskeyRequiresAWriteableMember() {
		User alice = member(Role.ADMIN, UserStatus.SUSPENDED);
		when(current.require()).thenReturn(alice);
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

		assertThatThrownBy(() -> resolver.revokePasskey("cred"))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
		verify(passkeys, never()).revoke(any(), anyString());
	}

	@Test
	void renamePasskeyDelegatesToAnActiveMember() {
		User alice = member(Role.ADMIN, UserStatus.ACTIVE);
		when(current.require()).thenReturn(alice);
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
		PasskeyDto dto = new PasskeyDto("cred", "label", null, null);
		when(passkeys.rename(any(), eq("cred"), eq("label"))).thenReturn(dto);

		assertThat(resolver.renamePasskey("cred", "label")).isEqualTo(dto);
		verify(passkeys).rename(alice, "cred", "label");
	}

	@Test
	void revokePasskeyDelegatesToAnActiveMember() {
		User alice = member(Role.ADMIN, UserStatus.ACTIVE);
		when(current.require()).thenReturn(alice);
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

		assertThat(resolver.revokePasskey("cred")).isTrue();
		verify(passkeys).revoke(alice, "cred");
	}

	private User member(Role role, UserStatus status) {
		User user = new User();
		user.setUsername("alice");
		user.setRole(role);
		user.setStatus(status);
		return user;
	}
}
