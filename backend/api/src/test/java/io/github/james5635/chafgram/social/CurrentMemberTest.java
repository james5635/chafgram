package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.identity.LastSeenService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserRepository;
import io.github.james5635.chafgram.identity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentMemberTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void requireResolvesTheAuthenticatedMember() {
		UserRepository users = mock(UserRepository.class);
		User alice = member();
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
		authenticate("alice");

		CurrentMember current = new CurrentMember(users, mock(LastSeenService.class));

		assertThat(current.require()).isSameAs(alice);
	}

	/** FR-2.5: reading the API is activity — the presence must be refreshed. */
	@Test
	void requireRefreshesThePresence() {
		UserRepository users = mock(UserRepository.class);
		LastSeenService lastSeen = mock(LastSeenService.class);
		User alice = member();
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
		authenticate("alice");

		new CurrentMember(users, lastSeen).require();

		verify(lastSeen).touch(alice.getId());
	}

	/**
	 * Presence is best-effort: a failing last-seen write (including one that
	 * only fails at commit) must not break the read that triggered it.
	 */
	@Test
	void aFailingPresenceUpdateNeverBreaksTheRead() {
		UserRepository users = mock(UserRepository.class);
		LastSeenService lastSeen = mock(LastSeenService.class);
		User alice = member();
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
		doThrow(new IllegalStateException("presence write failed")).when(lastSeen).touch(anyLong());
		authenticate("alice");

		CurrentMember current = new CurrentMember(users, lastSeen);

		assertThatCode(() -> current.require()).doesNotThrowAnyException();
		assertThat(current.require()).isSameAs(alice);
	}

	private void authenticate(String username) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(username, "n/a", List.of()));
	}

	private User member() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 7L);
		user.setUsername("alice");
		user.setRole(Role.ADMIN);
		user.setStatus(UserStatus.ACTIVE);
		return user;
	}
}
