package io.github.james5635.chafgram.notifications;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.social.service.PostService;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.repository.PostRepository;
import io.github.james5635.chafgram.social.PostType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.james5635.chafgram.notifications.service.NotificationService;

/**
 * Notification flow: FR-7.2 mark-read, BR-7.3 tombstone hiding, badge parity.
 */
@AutoConfigureMockMvc
class NotificationFlowIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private PostService posts;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private NotificationService notifications;

	User admin;
	User member;

	@BeforeEach
	void setUp() {
		wipeSocial();
		admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		CreateInviteResponse invite = inviteService.create(null, Role.MEMBER, admin);
		member = inviteService.accept(
				invite.token(), "bob", "Bob", "bob@example.com", "bob-very-strong-password");
	}

	private Long notifyAndGetId(User recipient, User actor, NotificationType type,
			NotificationEntityType entityType, Long entityId) {
		notifications.notify(recipient.getId(), actor.getId(), type, entityType, entityId);
		return notifications.list(recipient.getId(), 1).get(0).getId();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void markNotificationRead() {
		Post post = posts.create(member, PostType.NOTE, "Hello world", List.of());
		Long notificationId = notifyAndGetId(admin, member, NotificationType.LIKE,
				NotificationEntityType.POST, post.getId());

		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(1);

		notifications.markRead(admin, notificationId);

		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(0);
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void markNotificationReadRequiresOwnership() {
		Post post = posts.create(member, PostType.NOTE, "Hello world", List.of());
		Long notificationId = notifyAndGetId(admin, member, NotificationType.LIKE,
				NotificationEntityType.POST, post.getId());

		assertThatThrownBy(() -> notifications.markRead(member, notificationId))
				.isInstanceOf(ApiException.class);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void tombstonedPostHidesNotification() {
		Post post = posts.create(member, PostType.NOTE, "Hello world", List.of());
		notifications.notify(admin.getId(), member.getId(), NotificationType.LIKE,
				NotificationEntityType.POST, post.getId());

		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(1);
		assertThat(notifications.list(admin.getId(), 20)).hasSize(1);

		posts.delete(admin, post.getId()); // tombstone

		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(0);
		assertThat(notifications.list(admin.getId(), 20)).isEmpty();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void notificationForPurgedPostIsDropped() {
		Post post = posts.create(member, PostType.NOTE, "Hello world", List.of());
		notifications.notify(admin.getId(), member.getId(), NotificationType.LIKE,
				NotificationEntityType.POST, post.getId());

		// Purge the post row directly (no FK on notifications)
		postRepository.deleteById(post.getId());

		assertThat(notifications.list(admin.getId(), 20)).isEmpty();
		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(0);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void nonTombstonedTargetsUnaffected() {
		Post post = posts.create(member, PostType.NOTE, "Hello world", List.of());
		notifications.notify(admin.getId(), member.getId(), NotificationType.FOLLOW,
				NotificationEntityType.USER, member.getId());

		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(1);
		assertThat(notifications.list(admin.getId(), 20)).hasSize(1);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void markNotificationReadViaGraphQl() throws Exception {
		Post post = posts.create(member, PostType.NOTE, "Hello world", List.of());
		Long notificationId = notifyAndGetId(admin, member, NotificationType.LIKE,
				NotificationEntityType.POST, post.getId());

		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(1);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { markNotificationRead(id: " + notificationId + ") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.markNotificationRead").value(true));

		assertThat(notifications.unreadCount(admin.getId())).isEqualTo(0);
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void markNotificationReadViaGraphQlRequiresOwnership() throws Exception {
		Post post = posts.create(member, PostType.NOTE, "Hello world", List.of());
		Long notificationId = notifyAndGetId(admin, member, NotificationType.LIKE,
				NotificationEntityType.POST, post.getId());

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { markNotificationRead(id: " + notificationId + ") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")));
	}

	/**
	 * The list has no cursor: a page full of hidden notifications must not
	 * swallow the older visible ones — keep scanning until the requested
	 * number of visible rows is collected.
	 */
	@Test
	void listFillsThePagePastTombstonedNotifications() {
		Post visible = posts.create(member, PostType.NOTE, "Visible", List.of());
		notifications.notify(admin.getId(), member.getId(), NotificationType.LIKE,
				NotificationEntityType.POST, visible.getId());

		Post tombstoned = posts.create(member, PostType.NOTE, "Tombstoned", List.of());
		tombstoned.setDeletedAt(java.time.Instant.now());
		postRepository.save(tombstoned);
		notifications.notify(admin.getId(), member.getId(), NotificationType.LIKE,
				NotificationEntityType.POST, tombstoned.getId());

		List<Notification> page = notifications.listVisible(admin, 1);

		assertThat(page).extracting(Notification::getEntityId).containsExactly(visible.getId());
	}

	@Test
	void unreadCountIgnoresTombstonedTargets() {
		Post visible = posts.create(member, PostType.NOTE, "Visible", List.of());
		notifications.notify(admin.getId(), member.getId(), NotificationType.LIKE,
				NotificationEntityType.POST, visible.getId());

		Post tombstoned = posts.create(member, PostType.NOTE, "Tombstoned", List.of());
		tombstoned.setDeletedAt(java.time.Instant.now());
		postRepository.save(tombstoned);
		notifications.notify(admin.getId(), member.getId(), NotificationType.LIKE,
				NotificationEntityType.POST, tombstoned.getId());

		assertThat(notifications.countUnreadVisible(admin)).isEqualTo(1);
	}
}
