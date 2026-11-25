package io.github.james5635.chafgram.identity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.LastSeenService;
import io.github.james5635.chafgram.identity.service.MemberAccess;

class MemberAccessTest {

	@Test
	void currentMemberResolvesTheMember() {
		UserRepository users = mock(UserRepository.class);
		LastSeenService lastSeen = mock(LastSeenService.class);
		User alice = member();
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

		assertThat(new MemberAccess(users, lastSeen).currentMember("alice")).isSameAs(alice);
	}

	/** FR-2.5: resolving the member counts as activity. */
	@Test
	void currentMemberRefreshesThePresence() {
		UserRepository users = mock(UserRepository.class);
		LastSeenService lastSeen = mock(LastSeenService.class);
		User alice = member();
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

		new MemberAccess(users, lastSeen).currentMember("alice");

		verify(lastSeen).touch(alice.getId());
	}

	/**
	 * Presence is best-effort: a failing last-seen write (including one that
	 * only fails at commit) must not break the request that triggered it.
	 */
	@Test
	void aFailingPresenceUpdateNeverBreaksTheRequest() {
		UserRepository users = mock(UserRepository.class);
		LastSeenService lastSeen = mock(LastSeenService.class);
		User alice = member();
		when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
		doThrow(new IllegalStateException("presence write failed")).when(lastSeen).touch(anyLong());

		MemberAccess access = new MemberAccess(users, lastSeen);

		assertThatCode(() -> access.currentMember("alice")).doesNotThrowAnyException();
		assertThat(access.currentMember("alice")).isSameAs(alice);
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
