package io.github.james5635.chafgram.social;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.InviteService;
import io.github.james5635.chafgram.identity.MemberService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.social.dto.Views;
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

import java.time.Instant;
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
	@Autowired CommentRepository commentRepository;
	@Autowired Views views;
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
		assertThatThrownBy(() -> messaging.send(member, admin.getId(), "hi", List.of())).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> messaging.send(admin, member.getId(), "hi", List.of())).isInstanceOf(ApiException.class);

		blocks.unblock(member, admin.getId());
		assertThat(feed.discoverFeed(admin, 20, Long.MAX_VALUE)).hasSize(1);
	}

	@Test
	void discoverFeedFillsPagePastBlockedPosts() {
		Post visible = posts.create(member, PostType.NOTE, "Visible older post", List.of());
		posts.create(admin, PostType.NOTE, "Hidden newer post", List.of());
		blocks.block(member, admin.getId());

		assertThat(feed.discoverFeed(member, 1, Long.MAX_VALUE))
				.extracting(Post::getId)
				.containsExactly(visible.getId());
	}

	@Test
	void editingANoteCannotRemoveItsRequiredBody() {
		Post note = posts.create(member, PostType.NOTE, "A note needs text", List.of());

		assertThatThrownBy(() -> posts.update(member, note.getId(), "  "))
				.isInstanceOf(ApiException.class);
		assertThat(posts.get(note.getId()).getBody()).isEqualTo("A note needs text");
	}

	@Test
	void deletedCommentsRenderAsRemoved() {
		Post post = posts.create(member, PostType.NOTE, "A post with a comment", List.of());
		Comment comment = comments.add(member, post.getId(), null, "This should be hidden");
		comments.delete(member, comment.getId());

		Comment deleted = commentRepository.findById(comment.getId()).orElseThrow();
		assertThat(views.comment(deleted, admin).body()).isEqualTo("[removed]");
	}

	@Test
	void watchFeedHidesVideosOfMembersWhoBlockedTheViewer() {
		// alice (admin) blocks bob (member) — bob must not see alice's videos
		MediaAsset shortVideo = new MediaAsset();
		shortVideo.setOwnerUserId(admin.getId());
		shortVideo.setKind(MediaKind.VIDEO);
		shortVideo.setStatus(MediaStatus.READY);
		shortVideo.setStorageKey("test/blocked-short.mp4");
		shortVideo.setMimeType("video/mp4");
		shortVideo.setDurationMs(10000);
		shortVideo.setWidth(320);
		shortVideo.setHeight(240);
		shortVideo.setSizeBytes(1000L);
		mediaAssets.save(shortVideo);

		Post blockedVideo = posts.create(admin, PostType.VIDEO, "Alice's short video", List.of(shortVideo.getId()));
		// compare by id — the feed reloads the rows, so entity identity differs
		assertThat(feed.watchFeed(member, 10, Long.MAX_VALUE))
				.extracting(Post::getId)
				.contains(blockedVideo.getId());

		// BR-2.2: blocking is mutual-effect — the blocker also disappears
		blocks.block(admin, member.getId());
		assertThat(feed.watchFeed(member, 10, Long.MAX_VALUE)).isEmpty();
	}

	@Test
	void followingFeedHidesPostsOfMembersWhoBlockedTheViewer() {
		follows.follow(member, admin.getId());
		Post mine = posts.create(admin, PostType.NOTE, "Alice's post", List.of());
		// compare by id — the feed reloads the rows, so entity identity differs
		assertThat(feed.followingFeed(member, 10, Long.MAX_VALUE))
				.extracting(Post::getId)
				.contains(mine.getId());

		// BR-2.2: blocking is mutual-effect in both directions
		blocks.block(admin, member.getId());
		assertThat(feed.followingFeed(member, 10, Long.MAX_VALUE)).isEmpty();
	}

	@Test
	void watchFeedReturnsOnlyShortReadyVideos() {
		// Create test media assets directly
		MediaAsset shortVideo = new MediaAsset();
		shortVideo.setOwnerUserId(member.getId());
		shortVideo.setKind(MediaKind.VIDEO);
		shortVideo.setStatus(MediaStatus.READY);
		shortVideo.setStorageKey("test/short.mp4");
		shortVideo.setMimeType("video/mp4");
		shortVideo.setDurationMs(10000); // 10s <= 60s
		shortVideo.setWidth(320);
		shortVideo.setHeight(240);
		shortVideo.setSizeBytes(1000L);
		mediaAssets.save(shortVideo);

		MediaAsset longVideo = new MediaAsset();
		longVideo.setOwnerUserId(member.getId());
		longVideo.setKind(MediaKind.VIDEO);
		longVideo.setStatus(MediaStatus.READY);
		longVideo.setStorageKey("test/long.mp4");
		longVideo.setMimeType("video/mp4");
		longVideo.setDurationMs(90000); // 90s > 60s
		longVideo.setWidth(1920);
		longVideo.setHeight(1080);
		longVideo.setSizeBytes(10000L);
		mediaAssets.save(longVideo);

		MediaAsset processingVideo = new MediaAsset();
		processingVideo.setOwnerUserId(member.getId());
		processingVideo.setKind(MediaKind.VIDEO);
		processingVideo.setStatus(MediaStatus.PROCESSING);
		processingVideo.setStorageKey("test/processing.mp4");
		processingVideo.setMimeType("video/mp4");
		processingVideo.setDurationMs(30000);
		processingVideo.setWidth(640);
		processingVideo.setHeight(480);
		processingVideo.setSizeBytes(5000L);
		mediaAssets.save(processingVideo);

		// Create posts with each media
		Post shortPost = posts.create(member, PostType.VIDEO, "Short video", List.of(shortVideo.getId()));
		Post longPost = posts.create(member, PostType.VIDEO, "Long video", List.of(longVideo.getId()));
		Post procPost = posts.create(member, PostType.VIDEO, "Processing video", List.of(processingVideo.getId()));
		Post notePost = posts.create(member, PostType.NOTE, "Just a note", List.of());

		// watchFeed should only return the short ready video (<= 60s)
		var watch = feed.watchFeed(admin, 10, Long.MAX_VALUE);
		assertThat(watch).hasSize(1);
		assertThat(watch.get(0).getId()).isEqualTo(shortPost.getId());
	}

	/**
	 * BR-2.2 pagination: the newest video is blocked, so a page that fills
	 * with it must still reach the older allowed videos — the next cursor
	 * cannot stop on a hidden row, or they become unreachable.
	 */
	@Test
	void watchFeedFillsThePagePastBlockedVideos() {
		MediaAsset allowed = shortVideo(member, "watch-allowed.mp4");
		Post allowedPost = posts.create(member, PostType.VIDEO, "Allowed video", List.of(allowed.getId()));
		MediaAsset blocked = shortVideo(admin, "watch-blocked.mp4");
		posts.create(admin, PostType.VIDEO, "Blocked video", List.of(blocked.getId()));

		// member blocks admin — mutual-effect: admin's videos are hidden from member
		blocks.block(member, admin.getId());

		assertThat(feed.watchFeed(member, 1, Long.MAX_VALUE))
				.extracting(Post::getId)
				.containsExactly(allowedPost.getId());
	}

	private MediaAsset shortVideo(User owner, String key) {
		MediaAsset video = new MediaAsset();
		video.setOwnerUserId(owner.getId());
		video.setKind(MediaKind.VIDEO);
		video.setStatus(MediaStatus.READY);
		video.setStorageKey("test/" + key);
		video.setMimeType("video/mp4");
		video.setDurationMs(10000);
		video.setWidth(320);
		video.setHeight(240);
		video.setSizeBytes(1000L);
		mediaAssets.save(video);
		return video;
	}

	@Test
	void searchPostsWithKindFilter() {
		MediaAsset shortVideo = new MediaAsset();
		shortVideo.setOwnerUserId(member.getId());
		shortVideo.setKind(MediaKind.VIDEO);
		shortVideo.setStatus(MediaStatus.READY);
		shortVideo.setStorageKey("test/short.mp4");
		shortVideo.setMimeType("video/mp4");
		shortVideo.setDurationMs(10000);
		shortVideo.setWidth(320);
		shortVideo.setHeight(240);
		shortVideo.setSizeBytes(1000L);
		mediaAssets.save(shortVideo);

		MediaAsset photoAsset = new MediaAsset();
		photoAsset.setOwnerUserId(member.getId());
		photoAsset.setKind(MediaKind.IMAGE);
		photoAsset.setStatus(MediaStatus.READY);
		photoAsset.setStorageKey("test/photo.png");
		photoAsset.setMimeType("image/png");
		photoAsset.setWidth(640);
		photoAsset.setHeight(480);
		photoAsset.setSizeBytes(2000L);
		mediaAssets.save(photoAsset);

		Post videoPost = posts.create(member, PostType.VIDEO, "Video post", List.of(shortVideo.getId()));
		Post photoPost = posts.create(member, PostType.PHOTO, "Photo post", List.of(photoAsset.getId()));
		Post notePost = posts.create(member, PostType.NOTE, "Note post", List.of());

		// Search with kind VIDEO
		var videoResults = feed.search(admin, "post", 20, PostType.VIDEO);
		assertThat(videoResults).hasSize(1);
		assertThat(videoResults.get(0).getId()).isEqualTo(videoPost.getId());

		// Search with kind PHOTO
		var photoResults = feed.search(admin, "post", 20, PostType.PHOTO);
		assertThat(photoResults).hasSize(1);
		assertThat(photoResults.get(0).getId()).isEqualTo(photoPost.getId());

		// Search with kind NOTE
		var noteResults = feed.search(admin, "post", 20, PostType.NOTE);
		assertThat(noteResults).hasSize(1);
		assertThat(noteResults.get(0).getId()).isEqualTo(notePost.getId());

		// Search without kind filter returns all
		var allResults = feed.search(admin, "post", 20, null);
		assertThat(allResults).hasSize(3);
	}

	@Test
	void messagingFlowBetweenMembers() {
		Message message = messaging.send(admin, member.getId(), "Welcome to Chafgram, Bob!", List.of());
		assertThat(message.getType().name()).isEqualTo("TEXT");

		List<Conversation> convs = messaging.list(member, 20);
		assertThat(convs).hasSize(1);
		Long conversationId = convs.get(0).getId();

		assertThat(messaging.unreadCount(member, conversationId)).isEqualTo(1);
		messaging.markRead(member, conversationId);
		assertThat(messaging.unreadCount(member, conversationId)).isEqualTo(0);

		// bob muted the conversation → unread is 0 even with new messages
		messaging.send(admin, member.getId(), "ping", List.of());
		messaging.mute(member, conversationId, Instant.now().plusSeconds(3600));
		assertThat(messaging.unreadCount(member, conversationId)).isEqualTo(0);
	}
}
