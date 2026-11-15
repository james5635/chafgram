package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.InviteService;
import io.github.james5635.chafgram.identity.MemberService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.common.AuditService;
import io.github.james5635.chafgram.common.AuditLogRepository;
import io.github.james5635.chafgram.moderation.Report;
import io.github.james5635.chafgram.moderation.ReportReason;
import io.github.james5635.chafgram.moderation.ReportService;
import io.github.james5635.chafgram.moderation.ReportStatus;
import io.github.james5635.chafgram.moderation.ReportTargetType;
import io.github.james5635.chafgram.notifications.Notification;
import io.github.james5635.chafgram.notifications.NotificationService;
import io.github.james5635.chafgram.notifications.NotificationType;
import io.github.james5635.chafgram.messaging.MessagingService;
import io.github.james5635.chafgram.messaging.Conversation;
import io.github.james5635.chafgram.messaging.Message;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.MediaAssetRepository;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.MediaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end social flow: bootstrap → post → follow → react → comment → report →
 * moderate → block. Runs against the disposable dev database (cleaned each test).
 */
class SocialFlowIntegrationTest extends AbstractIntegrationTest {

	@Autowired MemberService memberService;
	@Autowired InviteService inviteService;
	@Autowired WorkspaceSettingsRepository settings;

	@Autowired PostService posts;
	@Autowired PostRepository postRepository;
	@Autowired CommentService comments;
	@Autowired FollowService follows;
	@Autowired BlockService blocks;
	@Autowired ReactionService reactions;
	@Autowired FeedService feed;
	@Autowired NotificationService notifications;
	@Autowired ReportService reports;
	@Autowired AuditLogRepository auditLog;
	@Autowired MessagingService messaging;
	@Autowired MediaAssetRepository mediaAssets;

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

	@Test
	void postFeedReactCommentReportModerateFlow() {
		// bob posts; alice (not following him yet) doesn't see it in her following feed
		Post mine = posts.create(member, PostType.NOTE, "Whiteboard from planning — shipping the feed next sprint", List.of());
		assertThat(feed.followingFeed(admin, 20, Long.MAX_VALUE)).isEmpty();

		// alice follows bob → feed shows bob's post
		follows.follow(admin, member.getId());
		assertThat(feed.followingFeed(admin, 20, Long.MAX_VALUE)).hasSize(1);

		// bob can't follow himself (BR-2.1)
		assertThatThrownBy(() -> follows.follow(member, member.getId()))
				.isInstanceOf(ApiException.class)
				.satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));

		// reactions: switching type keeps one row per user (BR-3.4)
		reactions.reactToPost(admin, mine.getId(), ReactionType.LIKE);
		reactions.reactToPost(member, mine.getId(), ReactionType.LOVE);
		reactions.reactToPost(member, mine.getId(), ReactionType.WOW);
		assertThat(reactions.postReactionCount(mine.getId())).isEqualTo(2);
		assertThat(reactions.myPostReaction(member, mine.getId())).isPresent()
				.get().hasToString("WOW");

		// comments: one level of threading only (BR-4.1)
		Comment top = comments.add(admin, mine.getId(), null, "Nice!");
		Comment reply = comments.add(member, mine.getId(), top.getId(), "Thanks!");
		assertThat(reply.getParentId()).isEqualTo(top.getId());
		assertThatThrownBy(() -> comments.add(member, mine.getId(), reply.getId(), "Nested?"))
				.isInstanceOf(ApiException.class);

		// bob is notified about alice's comment (not about his own reply target)
		List<Notification> bobNotifications = notifications.list(member.getId(), 20);
		assertThat(bobNotifications)
				.extracting(Notification::getType)
				.contains(NotificationType.COMMENT);

		// save/unsave round trip
		posts.save(admin, mine.getId());
		assertThat(posts.savedBy(admin.getId(), 20)).hasSize(1);
		posts.unsave(admin, mine.getId());
		assertThat(posts.savedBy(admin.getId(), 20)).isEmpty();

		// search finds the post (FULLTEXT)
		assertThat(feed.search(admin, "Whiteboard", 20)).hasSize(1);
		assertThat(feed.search(admin, "definitely-not-here", 20)).isEmpty();
	}

	@Test
	void staffDeletesWriteAuditLogAndTombstone() {
		Post mine = posts.create(member, PostType.NOTE, "A post for moderation", List.of());
		posts.delete(admin, mine.getId()); // admin, not the author (BR-3.5)
		assertThat(auditLog.findAll()).extracting("action").contains("post.delete");
		assertThatThrownBy(() -> posts.get(mine.getId())).isInstanceOf(ApiException.class);
	}

	@Test
	void reportActionDelegatesToModeration() {
		Post mine = posts.create(member, PostType.NOTE, "Report me please", List.of());
		Report report = reports.file(admin, ReportTargetType.POST, mine.getId(), ReportReason.SPAM, "look like spam");
		assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
		assertThat(reports.openQueue(admin)).hasSize(1);

		// a member must not see the queue
		assertThatThrownBy(() -> reports.openQueue(member)).isInstanceOf(ApiException.class);

		// actioned resolution tombstones the post and writes the audit log
		reports.resolve(admin, report.getId(), ReportStatus.ACTIONED, "removed after review");
		assertThat(auditLog.findAll()).extracting("action").contains("report.resolve");
		assertThatThrownBy(() -> posts.get(mine.getId())).isInstanceOf(ApiException.class);
	}

	@Test
	void reactionsEmitNotifications() {
		Post mine = posts.create(admin, PostType.NOTE, "React to me", List.of());
		reactions.reactToPost(member, mine.getId(), ReactionType.LOVE);
		assertThat(notifications.list(admin.getId(), 20))
				.extracting(Notification::getType)
				.contains(NotificationType.LIKE);
	}

	@Test
	void blocksHideContentAndMessages() {
		Post mine = posts.create(member, PostType.NOTE, "Blocked viewer cannot see this", List.of());
		blocks.block(member, admin.getId()); // Bob blocks Alice
		assertThat(feed.discoverFeed(admin, 20, Long.MAX_VALUE)).isEmpty(); // Alice can't see his post
		assertThatThrownBy(() -> messaging.send(member, admin.getId(), "hi")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> messaging.send(admin, member.getId(), "hi")).isInstanceOf(ApiException.class);

		blocks.unblock(member, admin.getId());
		assertThat(feed.discoverFeed(admin, 20, Long.MAX_VALUE)).hasSize(1);
	}

	@Test
	void messagingFlowBetweenMembers() {
		Message message = messaging.send(admin, member.getId(), "Welcome to Chafgram, Bob!");
		assertThat(message.getType().name()).isEqualTo("TEXT");

		List<Conversation> convs = messaging.list(member, 20);
		assertThat(convs).hasSize(1);
		Long conversationId = convs.get(0).getId();

		assertThat(messaging.unreadCount(member, conversationId)).isEqualTo(1);
		messaging.markRead(member, conversationId);
		assertThat(messaging.unreadCount(member, conversationId)).isEqualTo(0);

		// bob muted the conversation → unread is 0 even with new messages
		messaging.send(admin, member.getId(), "ping");
		messaging.mute(member, conversationId, java.time.Instant.now().plusSeconds(3600));
		assertThat(messaging.unreadCount(member, conversationId)).isEqualTo(0);
	}
}
