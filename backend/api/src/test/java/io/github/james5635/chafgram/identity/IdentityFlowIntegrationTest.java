package io.github.james5635.chafgram.identity;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.identity.dto.UpdateWorkspaceRequest;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.MediaStatus;
import io.github.james5635.chafgram.social.dto.MemberDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.james5635.chafgram.identity.repository.InviteRepository;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.repository.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberAccess;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.service.MemberUserDetailsService;
import io.github.james5635.chafgram.identity.service.WorkspaceService;
import io.github.james5635.chafgram.identity.utils.Tokens;

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

	@Autowired
	private MediaAssetRepository mediaAssets;

	@Autowired
	private MemberAccess memberAccess;

	@Autowired
	private MockMvc mockMvc;

	/** Each test gets a clean slate (same Spring context and database within the class). */
	@BeforeEach
	void clean() {
		invites.deleteAll();
		mediaAssets.deleteAll();
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

		UpdateWorkspaceRequest makeOpen = new UpdateWorkspaceRequest(null, null, null, "open", null);
		workspaceService.update("alice", makeOpen);

		User registered = memberService.registerOpen(
				"outsider", "Outsider", "outsider@example.com", null);
		assertThat(registered.getRole()).isEqualTo(Role.MEMBER);

		// back to invite_only for hygiene of other tests sharing this database
		workspaceService.update("alice", new UpdateWorkspaceRequest(null, null, null, "invite_only", null));
		WorkspaceSettings restored = settings.findById(WorkspaceSettings.SINGLETON_ID).orElseThrow();
		assertThat(restored.getSignupMode()).isEqualTo(SignupMode.INVITE_ONLY);
	}

	@Test
	void passwordLoginAcceptsEmailAsWellAsUsername() {
		memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		assertThat(userDetailsService.loadUserByUsername("alice").getUsername()).isEqualTo("alice");
		assertThat(userDetailsService.loadUserByUsername("alice@example.com").getUsername())
				.isEqualTo("alice");
		assertThat(userDetailsService.loadUserByUsername("  ALICE@EXAMPLE.COM  ").getUsername())
				.isEqualTo("alice");
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

	@Test
	void usernamesAreStoredLowercasedAndCompareCaseInsensitively() {
		User mixed = memberService.bootstrap(
				"MiXeD-Case", "Mixed", "mixed@example.com", "a-very-strong-password");
		assertThat(mixed.getUsername()).isEqualTo("mixed-case");
		assertThat(users.findByUsername("mixed-case")).isPresent();

		// same name in different case is a conflict (BR-1.2)
		workspaceService.update("mixed-case", new UpdateWorkspaceRequest(null, null, null, "open", null));
		assertThatThrownBy(() -> memberService.registerOpen(
				"MIXED-CASE", "Other", "other@example.com", null))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void lastSeenIsUpdatedThrottledToOneWritePerFiveMinutes() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		jdbc.update("UPDATE users SET last_seen_at = ? WHERE id = ?",
				Timestamp.from(Instant.now().minusSeconds(600)), alice.getId());

		memberAccess.currentMember("alice");
		Instant afterFirst = lastSeenOf(alice.getId());
		assertThat(afterFirst).isAfter(Instant.now().minusSeconds(60));

		memberAccess.currentMember("alice");
		assertThat(lastSeenOf(alice.getId())).isEqualTo(afterFirst);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void authenticatedGraphQlReadRefreshesLastSeen() throws Exception {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		jdbc.update("UPDATE users SET last_seen_at = ? WHERE id = ?",
				Timestamp.from(Instant.now().minusSeconds(600)), alice.getId());

		// FR-2.5: reading the API is activity — the presence must move
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ me { username } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.me.username", is("alice")));

		assertThat(lastSeenOf(alice.getId())).isAfter(Instant.now().minusSeconds(60));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void lastSeenIsExposedOnTheMemberDto() throws Exception {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		jdbc.update("UPDATE users SET last_seen_at = ? WHERE id = ?",
				Timestamp.from(Instant.now().minusSeconds(600)), alice.getId());

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ me { username lastSeenAt } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.me.username", is("alice")))
				.andExpect(jsonPath("$.data.me.lastSeenAt", notNullValue()));
	}

	private Instant lastSeenOf(Long userId) {
		Timestamp ts = jdbc.queryForObject(
				"SELECT last_seen_at FROM users WHERE id = ?", Timestamp.class, userId);
		return ts == null ? null : ts.toInstant();
	}

	@Test
	void updateProfileChangesDisplayNameAndBio() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		MemberDto updated = memberService.updateProfile(alice.getUsername(), "Alice B", "hello bio", null);

		assertThat(updated.displayName()).isEqualTo("Alice B");
		assertThat(updated.bio()).isEqualTo("hello bio");
		assertThat(users.findByUsername("alice").orElseThrow().getBio()).isEqualTo("hello bio");
	}

	@Test
	void updateProfileRejectsOversizedBio() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");

		String bio = "x".repeat(161);
		assertThatThrownBy(() -> memberService.updateProfile(alice.getUsername(), null, bio, null))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void updateProfileAcceptsOwnedReadyImageAsAvatar() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		MediaAsset avatar = imageAsset(alice, MediaStatus.READY);

		MemberDto updated = memberService.updateProfile(alice.getUsername(), null, null, avatar.getId());

		assertThat(updated.avatarMediaId()).isEqualTo(avatar.getId());
	}

	@Test
	void updateProfileRejectsVideoAsAvatar() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		MediaAsset video = imageAsset(alice, MediaStatus.READY);
		video.setKind(MediaKind.VIDEO);
		mediaAssets.save(video);

		assertThatThrownBy(() -> memberService.updateProfile(alice.getUsername(), null, null, video.getId()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void updateProfileRejectsAvatarOwnedByAnotherMember() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		CreateInviteResponse invite = inviteService.create(null, null, alice);
		User bob = inviteService.accept(
				invite.token(), "bob", "Bob", "bob@example.com", "bob-strong-password");
		MediaAsset bobsImage = imageAsset(bob, MediaStatus.READY);

		assertThatThrownBy(() -> memberService.updateProfile(alice.getUsername(), null, null, bobsImage.getId()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void updateWorkspaceSetsLogo() {
		User alice = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		MediaAsset logo = imageAsset(alice, MediaStatus.READY);

		workspaceService.update("alice", new UpdateWorkspaceRequest(null, null, null, null, logo.getId()));

		assertThat(settings.findById(WorkspaceSettings.SINGLETON_ID).orElseThrow().getLogoMediaId())
				.isEqualTo(logo.getId());
		assertThat(workspaceService.get().logoMediaId()).isEqualTo(logo.getId());
	}

	private MediaAsset imageAsset(User owner, MediaStatus status) {
		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(owner.getId());
		asset.setKind(MediaKind.IMAGE);
		asset.setStatus(status);
		asset.setStorageKey("test/" + owner.getUsername() + "-" + System.nanoTime() + ".png");
		asset.setMimeType("image/png");
		asset.setSizeBytes(10L);
		return mediaAssets.save(asset);
	}
}
