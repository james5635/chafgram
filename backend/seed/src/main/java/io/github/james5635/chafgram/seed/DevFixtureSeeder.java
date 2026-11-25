package io.github.james5635.chafgram.seed;

import io.github.james5635.chafgram.common.service.AuditService;
import io.github.james5635.chafgram.identity.AccountTokenType;
import io.github.james5635.chafgram.identity.Invite;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.InviteRepository;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.service.AccountFlowService;
import io.github.james5635.chafgram.identity.service.AccountTokenService;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.utils.Tokens;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.MediaStatus;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.messaging.service.MessagingService;
import io.github.james5635.chafgram.moderation.ReportReason;
import io.github.james5635.chafgram.moderation.ReportStatus;
import io.github.james5635.chafgram.moderation.ReportTargetType;
import io.github.james5635.chafgram.moderation.service.ReportService;
import io.github.james5635.chafgram.notifications.NotificationEntityType;
import io.github.james5635.chafgram.notifications.NotificationType;
import io.github.james5635.chafgram.notifications.service.NotificationService;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.ReactionType;
import io.github.james5635.chafgram.social.service.BlockService;
import io.github.james5635.chafgram.social.service.CommentService;
import io.github.james5635.chafgram.social.service.FollowService;
import io.github.james5635.chafgram.social.service.PostService;
import io.github.james5635.chafgram.social.service.ReactionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Dev-only fixtures, written through the same services the application uses.
 *
 * <p>Run via {@code just seed}, which resets the dev database before starting
 * this runner. Never enable this in a deployed profile.</p>
 */
@Component
public class DevFixtureSeeder implements ApplicationRunner {

    private static final String PASSWORD = "chafgram-dev-password";

    @Value("${chafgram.seed.enabled:true}")
    private boolean enabled;

    private final PlatformTransactionManager transactionManager;
    private final MemberService members;
    private final InviteService invites;
    private final InviteRepository inviteRepository;
    private final UserRepository users;
    private final AccountTokenService accountTokens;
    private final AccountFlowService accountFlows;
    private final MediaAssetRepository mediaAssets;
    private final PostService posts;
    private final CommentService comments;
    private final ReactionService reactions;
    private final FollowService follows;
    private final BlockService blocks;
    private final MessagingService messaging;
    private final NotificationService notifications;
    private final ReportService reports;
    private final AuditService audit;

    public DevFixtureSeeder(
            PlatformTransactionManager transactionManager,
            MemberService members,
            InviteService invites,
            InviteRepository inviteRepository,
            UserRepository users,
            AccountTokenService accountTokens,
            AccountFlowService accountFlows,
            MediaAssetRepository mediaAssets,
            PostService posts,
            CommentService comments,
            ReactionService reactions,
            FollowService follows,
            BlockService blocks,
            MessagingService messaging,
            NotificationService notifications,
            ReportService reports,
            AuditService audit) {
        this.transactionManager = transactionManager;
        this.members = members;
        this.invites = invites;
        this.inviteRepository = inviteRepository;
        this.users = users;
        this.accountTokens = accountTokens;
        this.accountFlows = accountFlows;
        this.mediaAssets = mediaAssets;
        this.posts = posts;
        this.comments = comments;
        this.reactions = reactions;
        this.follows = follows;
        this.blocks = blocks;
        this.messaging = messaging;
        this.notifications = notifications;
        this.reports = reports;
        this.audit = audit;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            System.out.println("Flyway migrations applied (seeding disabled).");
            return;
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> seed());
        System.out.println("Dev fixtures seeded (admin@chafgram.test / password: " + PASSWORD + ")");
    }

    private void seed() {
        if (users.count() > 0) {
            throw new IllegalStateException("Dev database is not empty — run `just db-reset` then `just migrate` first.");
        }

        User admin = members.bootstrap("admin", "Dev Admin", "admin@chafgram.test", PASSWORD);
        User maya = join(admin, "maya", "Maya", "maya@chafgram.test");
        User tao = join(admin, "tao", "Tao", "tao@chafgram.test");
        User lena = join(admin, "lena", "Lena", "lena@chafgram.test");

        reusableDemoInvite(admin);
        avatars(admin, maya, tao, lena);
        socialGraph(admin, maya, tao, lena);
        conversations(admin, maya, tao);
        notificationsAndModeration(admin, maya, lena);
        accountFlowsAndAudit(admin, maya);
    }

    private User join(User admin, String username, String displayName, String email) {
        var invite = invites.create(null, Role.MEMBER, admin);
        return invites.accept(invite.token(), username, displayName, email, PASSWORD);
    }

    void reusableDemoInvite(User admin) {
        Invite demo = new Invite();
        demo.setTokenHash(Tokens.sha256Hex("demo-invite-token"));
        demo.setRole(Role.MEMBER);
        demo.setCreatedBy(admin.getId());
        demo.setExpiresAt(Instant.now().plus(Duration.ofDays(365)));
        inviteRepository.save(demo);
    }

    private void avatars(User admin, User maya, User tao, User lena) {
        avatar(admin, "admin.png");
        avatar(maya, "maya.png");
        avatar(tao, "tao.png");
        avatar(lena, "lena.png");
    }

    private void avatar(User owner, String filename) {
        MediaAsset image = asset(owner, MediaKind.IMAGE, "demo/avatars/" + filename,
                "image/png", 256, 256, null, 48_213);
        owner.setAvatarMediaId(image.getId());
        users.save(owner);
    }

    private void socialGraph(User admin, User maya, User tao, User lena) {
        MediaAsset adminPostImage = asset(admin, MediaKind.IMAGE, "demo/posts/all-hands.png",
                "image/png", 1600, 900, null, 834_400);
        MediaAsset teaBoard = asset(maya, MediaKind.IMAGE, "demo/posts/tea-board.png",
                "image/png", 1600, 900, null, 834_400);
        MediaAsset agenda = asset(lena, MediaKind.IMAGE, "demo/posts/offsite-agenda.png",
                "image/png", 1600, 900, null, 902_144);
        MediaAsset shortVideo = asset(tao, MediaKind.VIDEO, "demo/shorts/walkthrough.mp4",
                "video/mp4", 720, 1280, 30_000, 6_214_400);
        MediaAsset longVideo = asset(admin, MediaKind.VIDEO, "demo/videos/all-hands-q3.mp4",
                "video/mp4", 1280, 720, 75_000, 18_432_000);

        var allHands = posts.create(admin, PostType.PHOTO, "All hands recap — great to see everyone! 🚀",
                List.of(adminPostImage.getId()));
        var status = posts.create(maya, PostType.NOTE, "Shipping the new feed today. Coffee first. ☕", List.of());
        var tea = posts.create(maya, PostType.PHOTO, "Whiteboard from this morning's session.",
                List.of(teaBoard.getId()));
        var walkthrough = posts.create(tao, PostType.VIDEO, "A quick walkthrough of the new workspace.",
                List.of(shortVideo.getId()));
        posts.create(admin, PostType.VIDEO, "Q3 all-hands recording.", List.of(longVideo.getId()));
        posts.create(lena, PostType.PHOTO, "Offsite agenda is up.", List.of(agenda.getId()));
        var removable = posts.create(maya, PostType.NOTE, "Temporary post for moderation demo.", List.of());

        follows.follow(maya, admin.getId());
        follows.follow(tao, maya.getId());
        follows.follow(lena, admin.getId());
        follows.follow(admin, maya.getId());

        var comment = comments.add(maya, allHands.getId(), null, "Great turnout!");
        comments.add(admin, allHands.getId(), comment.getId(), "Thanks — thanks for coming.");
        comments.add(tao, tea.getId(), null, "That whiteboard is excellent @maya");

        reactions.reactToPost(maya, allHands.getId(), ReactionType.LIKE);
        reactions.reactToPost(tao, allHands.getId(), ReactionType.LOVE);
        reactions.reactToPost(admin, status.getId(), ReactionType.LIKE);
        reactions.reactToPost(lena, walkthrough.getId(), ReactionType.WOW);
        reactions.reactToComment(admin, comment.getId(), ReactionType.LIKE);

        posts.save(lena, tea.getId());
        blocks.block(lena, tao.getId());
        moderationTarget = removable.getId();
        mentionPostId = status.getId();
    }

    private Long moderationTarget;
    private Long mentionPostId;

    private void conversations(User admin, User maya, User tao) {
        var whiteboard = asset(maya, MediaKind.IMAGE, "demo/messages/whiteboard.png",
                "image/png", 1600, 1200, null, 733_184);

        messaging.send(admin, maya.getId(), "Morning! Do you have a minute?", List.of());
        messaging.send(maya, admin.getId(), "Sure — just finished the feed work.", List.of());
        messaging.send(maya, admin.getId(), "Here's the whiteboard.", List.of(whiteboard.getId()));

        var group = messaging.createGroup(admin, "Launch crew", List.of(maya.getId(), tao.getId()));
        messaging.sendToConversation(admin, group.getId(), "Kickoff notes are in the shared drive.", List.of());
        messaging.sendToConversation(tao, group.getId(), "Thanks! I'll take the demo.", List.of());
    }

    private void notificationsAndModeration(User admin, User maya, User lena) {
        notifications.notify(lena.getId(), maya.getId(), NotificationType.MENTION,
                NotificationEntityType.POST, mentionPostId);
        notifications.notify(admin.getId(), null, NotificationType.SYSTEM,
                NotificationEntityType.USER, admin.getId());
        notifications.mute(maya.getId(), NotificationType.LIKE);

        reports.file(lena, ReportTargetType.POST, moderationTarget,
                ReportReason.SPAM, "Temporary moderation fixture");
        var actioned = reports.file(admin, ReportTargetType.POST, moderationTarget,
                ReportReason.HARASSMENT, "Tombstone for the moderation demo");
        reports.resolve(admin, actioned.getId(), ReportStatus.ACTIONED, "Post removed by seed demo");
    }

    private void accountFlowsAndAudit(User admin, User maya) {
        String token = accountTokens.create(maya.getId(), AccountTokenType.VERIFY_EMAIL);
        accountFlows.verifyEmail(token);
        audit.log(admin.getId(), "seed.fixtures", "workspace", 1L,
                Map.of("members", 4, "source", "seed-subproject"));
    }

    private MediaAsset asset(User owner, MediaKind kind, String storageKey, String mimeType,
            Integer width, Integer height, Integer durationMs, long sizeBytes) {
        MediaAsset asset = new MediaAsset();
        asset.setOwnerUserId(owner.getId());
        asset.setKind(kind);
        asset.setStatus(MediaStatus.READY);
        asset.setStorageKey(storageKey);
        asset.setMimeType(mimeType);
        asset.setWidth(width);
        asset.setHeight(height);
        asset.setDurationMs(durationMs);
        asset.setSizeBytes(sizeBytes);
        asset.setBlurhash("LEHV6nWB2yk8pyo0adR*.7kCMdnj");
        return mediaAssets.save(asset);
    }
}
