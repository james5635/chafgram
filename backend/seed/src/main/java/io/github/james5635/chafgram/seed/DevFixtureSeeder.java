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
import io.github.james5635.chafgram.media.utils.MediaStorage;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

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
    private final MediaStorage storage;
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
            MediaStorage storage,
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
        this.storage = storage;
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

    /** Five real photos shipped as classpath resources under images/. */
    private record ResourcePhoto(String filename, User author, String body) {
    }

    private void seedResourcePosts(User admin, User maya, User tao, User lena) {
        List<ResourcePhoto> photos = List.of(
                new ResourcePhoto("oranges-on-pink.webp", maya,
                        "Hydration break — fresh oranges for the whole team. 🍊"),
                new ResourcePhoto("forest-river.avif", tao,
                        "Weekend walk: where does this river go?"),
                new ResourcePhoto("coffee-cherries.jpg", admin,
                        "Coffee cherries on the branch — from the offsite farm visit."),
                new ResourcePhoto("portrait-grey-shirt.jpg", lena,
                        "Portrait from the team offsite — studio light turned out nicely."),
                new ResourcePhoto("mountain-lake-autumn.webp", maya,
                        "Autumn colours at the lake — what a view."),
                new ResourcePhoto("nomadic-traveler.jpeg", tao,
                        "Met a traveller on the road — what a portrait."),
                new ResourcePhoto("bioluminescent-forest.jpeg", maya,
                        "This forest at night looks unreal."),
                new ResourcePhoto("silver-gown-model.jpeg", lena,
                        "Editorial shoot from the weekend — silver reflections."),
                new ResourcePhoto("hot-cocoa-windowsill.jpeg", admin,
                        "Rainy windowsill, hot cocoa, tiny break. ☕"));

        for (ResourcePhoto photo : photos) {
            String storageKey = "demo/posts/" + photo.filename();
            MediaAsset asset = resourceAsset(photo.author(), storageKey, imageMimeType(photo.filename()));
            posts.create(photo.author(), PostType.PHOTO, photo.body(), List.of(asset.getId()));
        }
    }

    private MediaAsset resourceAsset(User owner, String storageKey, String mimeType) {
        long size = writeResourceImage(storageKey);
        MediaAsset asset = new MediaAsset();
        asset.setOwnerUserId(owner.getId());
        asset.setKind(MediaKind.IMAGE);
        asset.setStatus(MediaStatus.READY);
        asset.setStorageKey(storageKey);
        asset.setMimeType(mimeType);
        asset.setSizeBytes(size);
        asset.setBlurhash("LEHV6nWB2yk8pyo0adR*.7kCMdnj");
        return mediaAssets.save(asset);
    }

    private MediaAsset resourceVideoAsset(User owner, Resource resource, String storageKey, Integer durationMs) {
        long size = storeResource(storageKey, resource);
        MediaAsset asset = new MediaAsset();
        asset.setOwnerUserId(owner.getId());
        asset.setKind(MediaKind.VIDEO);
        asset.setStatus(MediaStatus.READY);
        asset.setStorageKey(storageKey);
        asset.setMimeType("video/mp4");
        asset.setDurationMs(durationMs);
        asset.setSizeBytes(size);
        return mediaAssets.save(asset);
    }

    private long storeResource(String storageKey, Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            Path temp = Files.createTempFile("chafgram-seed-", "-" + storageKey.replace('/', '_'));
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            long size = Files.size(temp);
            storage.storeProcessed(temp, storageKey);
            return size;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to seed blob " + storageKey, e);
        }
    }

    /** Seed every MP4 shipped under videos/ as a VIDEO post. */
    private record VideoFixture(String filename, String storageKey, String caption) {
    }

    /** Explicit list of MP4s shipped under videos/ that are seeded as posts. */
    private static final List<VideoFixture> VIDEO_FIXTURES = List.of(
            new VideoFixture("Dewdrop falling from a fern leaf in a misty forest.mp4",
                    "demo/videos/dewdrop-fern.mp4", "A dewdrop falling from a fern in the misty forest."),
            new VideoFixture("Futuristic sports car speeding down a neon-lit street.mp4",
                    "demo/videos/neon-sports-car.mp4", "Neon nights and a very fast car."),
            new VideoFixture("Medieval castle by a cliffside waterfall.mp4",
                    "demo/videos/castle-waterfall.mp4", "A castle by the cliffside waterfall."),
            new VideoFixture("Woman with gold geometric makeup laughing.mp4",
                    "demo/videos/gold-makeup.mp4", "Gold makeup, good laugh."),
            new VideoFixture("dog-eating-pizza.mp4",
                    "demo/videos/dog-eating-pizza.mp4", "Lunch break with the world's hungriest intern 🐶🍕"));

    private void seedResourceVideos(User admin, User maya, User tao, User lena) {
        List<User> authors = List.of(admin, maya, tao, lena);
        for (int i = 0; i < VIDEO_FIXTURES.size(); i += 1) {
            VideoFixture fixture = VIDEO_FIXTURES.get(i);
            Resource resource = new ClassPathResource("videos/" + fixture.filename());
            User author = authors.get(i % authors.size());
            MediaAsset asset = resourceVideoAsset(author, resource, fixture.storageKey(), 5_042);
            var post = posts.create(author, PostType.VIDEO, fixture.caption(), List.of(asset.getId()));
            if (i == 0) {
                likedVideoPostId = post.getId();
            }
        }
    }

    private long writeResourceImage(String storageKey) {
        String filename = storageKey.substring("demo/posts/".length());
        return writeClasspathBlob(storageKey, "images/" + filename);
    }

    private long writeClasspathBlob(String storageKey, String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing seed resource: " + resourcePath);
            }
            Path temp = Files.createTempFile("chafgram-seed-", "-" + resourcePath.replace('/', '_'));
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            long size = Files.size(temp);
            storage.storeProcessed(temp, storageKey);
            return size;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to seed blob " + storageKey, e);
        }
    }

    static String imageMimeType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".avif")) return "image/avif";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "image/png";
    }

    private void socialGraph(User admin, User maya, User tao, User lena) {
        MediaAsset adminPostImage = asset(admin, MediaKind.IMAGE, "demo/posts/all-hands.png",
                "image/png", 1600, 900, null, 834_400);
        MediaAsset teaBoard = asset(maya, MediaKind.IMAGE, "demo/posts/tea-board.png",
                "image/png", 1600, 900, null, 834_400);
        MediaAsset agenda = asset(lena, MediaKind.IMAGE, "demo/posts/offsite-agenda.png",
                "image/png", 1600, 900, null, 902_144);
        var allHands = posts.create(admin, PostType.PHOTO, "All hands recap — great to see everyone! 🚀",
                List.of(adminPostImage.getId()));
        var status = posts.create(maya, PostType.NOTE, "Shipping the new feed today. Coffee first. ☕", List.of());
        var tea = posts.create(maya, PostType.PHOTO, "Whiteboard from this morning's session.",
                List.of(teaBoard.getId()));
        posts.create(lena, PostType.PHOTO, "Offsite agenda is up.", List.of(agenda.getId()));
        var removable = posts.create(maya, PostType.NOTE, "Temporary post for moderation demo.", List.of());
        seedResourcePosts(admin, maya, tao, lena);
        seedResourceVideos(admin, maya, tao, lena);

        // Following feed is pull-based: follow everyone so the demo workspace
        // shows all seeded posts (including all videos) to every member.
        List<User> everyone = List.of(admin, maya, tao, lena);
        for (User follower : everyone) {
            for (User target : everyone) {
                if (!follower.getId().equals(target.getId())) {
                    follows.follow(follower, target.getId());
                }
            }
        }

        var comment = comments.add(maya, allHands.getId(), null, "Great turnout!");
        comments.add(admin, allHands.getId(), comment.getId(), "Thanks — thanks for coming.");
        comments.add(tao, tea.getId(), null, "That whiteboard is excellent @maya");

        reactions.reactToPost(maya, allHands.getId(), ReactionType.LIKE);
        reactions.reactToPost(tao, allHands.getId(), ReactionType.LOVE);
        reactions.reactToPost(admin, status.getId(), ReactionType.LIKE);
        reactions.reactToPost(lena, likedVideoPostId, ReactionType.WOW);
        reactions.reactToComment(admin, comment.getId(), ReactionType.LIKE);

        posts.save(lena, tea.getId());
        blocks.block(lena, tao.getId());
        moderationTarget = removable.getId();
        mentionPostId = status.getId();
    }

    private Long moderationTarget;
    private Long mentionPostId;
    private Long likedVideoPostId;

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
        long actualSize = sizeBytes;
        if (kind == MediaKind.IMAGE) {
            actualSize = writeDemoImage(storageKey, width, height);
        }
        MediaAsset asset = new MediaAsset();
        asset.setOwnerUserId(owner.getId());
        asset.setKind(kind);
        asset.setStatus(MediaStatus.READY);
        asset.setStorageKey(storageKey);
        asset.setMimeType(mimeType);
        asset.setWidth(width);
        asset.setHeight(height);
        asset.setDurationMs(durationMs);
        asset.setSizeBytes(actualSize);
        asset.setBlurhash("LEHV6nWB2yk8pyo0adR*.7kCMdnj");
        return mediaAssets.save(asset);
    }

    /** Seed a real PNG so the BFF media route has bytes to stream. */
    private long writeDemoImage(String storageKey, Integer width, Integer height) {
        int w = width == null || width <= 0 ? 256 : width;
        int h = height == null || height <= 0 ? 256 : height;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(Math.floorMod(storageKey.hashCode(), 0xFFFFFF)));
        graphics.fillRect(0, 0, w, h);
        graphics.setColor(Color.WHITE);
        graphics.drawString("Chafgram demo", 24, Math.min(48, h - 8));
        graphics.dispose();

        try {
            Path temp = Files.createTempFile("chafgram-seed-", ".png");
            ImageIO.write(image, "png", temp.toFile());
            long size = Files.size(temp);
            storage.storeProcessed(temp, storageKey);
            return size;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to seed demo image " + storageKey, e);
        }
    }
}
