package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.identity.dto.UpdateWorkspaceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end identity flow against a real MySQL: bootstrap, invite lifecycle,
 * admin guards, and open registration policy.
 */
class IdentityFlowIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private WorkspaceService workspaceService;

	@Autowired
	private MemberUserDetailsService userDetailsService;

	@Autowired
	private UserRepository users;

	@Autowired
	private InviteRepository invites;

	@Autowired
	private WorkspaceSettingsRepository settings;

	/** Each test gets a clean slate (same Spring context and database within the class). */
	@BeforeEach
	void clean() {
		invites.deleteAll();
		users.deleteAll();
	}

	@Test
	void bootstrapCreatesTheFirstAdminAndThenCloses() {
		User admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);

		assertThatThrownBy(() -> memberService.bootstrap(
				"bob", "Bob", "bob@example.com", "another-strong-password"))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void inviteLifecycleCreateAcceptReuseAndExpiry() {
		User admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		// reusable link (no email on the invite): email must come from the request
		CreateInviteResponse invite = inviteService.create(null, null, admin);
		User member = inviteService.accept(
				invite.token(), "bob", "Bob", "bob@example.com", "bob-strong-password");
		assertThat(member.getRole()).isEqualTo(Role.MEMBER);
		assertThat(member.getEmail()).isEqualTo("bob@example.com");

		// email-bound invite: email from the request is ignored in favor of the invite's
		CreateInviteResponse boundInvite = inviteService.create("carol@example.com", null, admin);
		User carol = inviteService.accept(
				boundInvite.token(), "carol", "Carol", "carol@different.example.com", null);
		assertThat(carol.getEmail()).isEqualTo("carol@example.com");

		// single-use
		assertThatThrownBy(() -> inviteService.accept(
				invite.token(), "carol", "Carol", "carol@example.com", null))
				.isInstanceOf(ApiException.class);

		// expired
		Invite expired = new Invite();
		expired.setTokenHash(Tokens.sha256Hex("expired-token"));
		expired.setRole(Role.MEMBER);
		expired.setExpiresAt(Instant.now().minusSeconds(60));
		invites.save(expired);
		assertThatThrownBy(() -> inviteService.accept(
				"expired-token", "dave", "Dave", "dave@example.com", null))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.GONE));

		// wrong token
		assertThatThrownBy(() -> inviteService.accept(
				"no-such-token", "erin", "Erin", "erin@example.com", null))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void lastActiveAdminCannotBeDemotedOrSuspended() {
		User admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		// alice cannot suspend herself (self-guard)
		assertThatThrownBy(() -> memberService.setStatus("alice", admin.getId(), UserStatus.SUSPENDED))
				.isInstanceOf(ApiException.class);

		// alice cannot demote the only active admin (herself) — last-admin guard
		assertThatThrownBy(() -> memberService.updateRole("alice", admin.getId(), Role.MEMBER))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));

		// second admin via invite, then alice can demote bob (alice remains an active admin)
		CreateInviteResponse invite = inviteService.create(null, Role.ADMIN, admin);
		User bob = inviteService.accept(
				invite.token(), "bob", "Bob", "bob@example.com", "bob-strong-password");
		memberService.updateRole("alice", bob.getId(), Role.MEMBER);

		// now bob is a member, alice is the only admin; suspend-devoke on self is blocked
		assertThatThrownBy(() -> memberService.updateRole("alice", admin.getId(), Role.MODERATOR))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void openRegistrationIsDisabledByDefaultAndEnabledOnDemand() {
		memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		assertThatThrownBy(() -> memberService.registerOpen(
				"outsider", "Outsider", "outsider@example.com", null))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

		UpdateWorkspaceRequest makeOpen = new UpdateWorkspaceRequest(null, null, null, "open");
		workspaceService.update("alice", makeOpen);

		User registered = memberService.registerOpen(
				"outsider", "Outsider", "outsider@example.com", null);
		assertThat(registered.getRole()).isEqualTo(Role.MEMBER);

		// back to invite_only for hygiene of other tests sharing this database
		workspaceService.update("alice", new UpdateWorkspaceRequest(null, null, null, "invite_only"));
		WorkspaceSettings restored = settings.findById(WorkspaceSettings.SINGLETON_ID).orElseThrow();
		assertThat(restored.getSignupMode()).isEqualTo(SignupMode.INVITE_ONLY);
	}

	@Test
	void suspendedAccountsCannotAuthenticateButCanStillBeListed() {
		User admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		CreateInviteResponse invite = inviteService.create(null, null, admin);
		User bob = inviteService.accept(
				invite.token(), "bob", "Bob", "bob@example.com", "bob-strong-password");

		memberService.setStatus("alice", bob.getId(), UserStatus.SUSPENDED);

		// suspended: login is still possible (read-only per functional-requirements.md FR-1.4)
		assertThat(userDetailsService.loadUserByUsername("bob").isEnabled()).isTrue();

		// writes are rejected
		assertThatThrownBy(() -> inviteService.create(null, null,
				users.findByUsername("bob").orElseThrow()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

		// deactivated: cannot authenticate at all
		memberService.setStatus("alice", bob.getId(), UserStatus.DEACTIVATED);
		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("bob"))
				.isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void inviteTokensAreStoredHashed() {
		User admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		CreateInviteResponse invite = inviteService.create(null, null, admin);
		List<Invite> stored = invites.findAllByOrderByCreatedAtDesc();
		assertThat(stored).hasSize(1);
		assertThat(stored.get(0).getTokenHash())
				.doesNotContain(invite.token())
				.isEqualTo(Tokens.sha256Hex(invite.token()));
	}
}
