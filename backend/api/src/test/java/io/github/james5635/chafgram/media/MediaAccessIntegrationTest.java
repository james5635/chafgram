package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.identity.UserStatus;
import io.github.james5635.chafgram.identity.repository.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.messaging.Message;
import io.github.james5635.chafgram.messaging.service.MessagingService;
import io.github.james5635.chafgram.social.PostMedia;
import io.github.james5635.chafgram.social.repository.PostMediaRepository;
import io.github.james5635.chafgram.social.service.PostService;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.repository.PostRepository;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.UserBlock;
import io.github.james5635.chafgram.social.repository.UserBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;

/**
 * Media bytes authorization tests (FR-5.4, BR-2.2, BR-3.1, BR-5.2).
 * All deny paths return identical 404 — no information leakage.
 */
@AutoConfigureMockMvc
class MediaAccessIntegrationTest extends AbstractIntegrationTest {

	/** Well above any message id — see {@link #createMediaWithExplicitId}. */
	private static final long FAR_ASSET_ID = 900_001L;

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
	private UserBlockRepository userBlocks;

	@Autowired
	private MediaAssetRepository mediaAssets;

	@Autowired
	private InMemoryMediaStorage storage;

	@Autowired
	private PostMediaRepository postMedia;

	@Autowired
	private UserRepository users;

	@Autowired
	private MessagingService messaging;

	User admin;
	User alice;
	User bob;

	@BeforeEach
	void setUp() {
		wipeSocial();
		admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		CreateInviteResponse invite = inviteService.create(null, Role.MEMBER, admin);
		alice = inviteService.accept(
				invite.token(), "alice2", "Alice2", "alice2@example.com", "alice2-very-strong-password");
		CreateInviteResponse invite2 = inviteService.create(null, Role.MEMBER, admin);
		bob = inviteService.accept(
				invite2.token(), "bob", "Bob", "bob@example.com", "bob-very-strong-password");
	}

	private MediaAsset createAndSaveMedia(User owner) {
		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(owner.getId());
		asset.setKind(MediaKind.IMAGE);
		asset.setStatus(MediaStatus.READY);
		String storageKey = "test-media/" + System.nanoTime() + ".png";
		asset.setStorageKey(storageKey);
		asset.setMimeType("image/png");
		asset.setSizeBytes(100L);

		storage.put(storageKey, new byte[]{1, 2, 3, 4});

		return mediaAssets.save(asset);
	}

	/**
	 * Creates a media asset with a caller-chosen id. The id sits far outside
	 * the message-id range on purpose: the byte authorization must look
	 * attachments up by media id, and a test must not pass just because an
	 * asset id happens to equal a message id.
	 */
	private MediaAsset createMediaWithExplicitId(User owner, long id) {
		String storageKey = "test-media/" + id + ".png";
		writeBlob(storageKey);
		jdbc.update("insert into media_assets "
				+ "(id, owner_user_id, kind, status, storage_key, mime_type, size_bytes) "
				+ "values (?, ?, 'image', 'ready', ?, 'image/png', 100)",
				id, owner.getId(), storageKey);
		return mediaAssets.findById(id).orElseThrow();
	}

	private void writeBlob(String storageKey) {
		storage.put(storageKey, new byte[]{1, 2, 3, 4});
	}

	private byte[] pngBytes() {
		return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==".getBytes(StandardCharsets.UTF_8);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void staffFetchesAnyMedia() throws Exception {
		MediaAsset asset = createAndSaveMedia(bob);

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void ownerFetchesOwnMedia() throws Exception {
		MediaAsset asset = createAndSaveMedia(alice);

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void nonMemberCannotFetchMedia() throws Exception {
		MediaAsset asset = createAndSaveMedia(bob);

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void tombstonedPostMediaIs404ForNonStaffMember() throws Exception {
		Post post = posts.create(bob, PostType.NOTE, "to be tombstoned", List.of());
		MediaAsset asset = createAndSaveMedia(bob);

		PostMedia pm = new PostMedia();
		pm.setPostId(post.getId());
		pm.setMediaId(asset.getId());
		pm.setPosition((short) 0);
		postMedia.save(pm);

		posts.delete(admin, post.getId()); // tombstone

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void blockedAuthorMediaIs404() throws Exception {
		// bob blocks alice2
		UserBlock block = new UserBlock();
		block.setBlockerId(bob.getId());
		block.setBlockedId(alice.getId());
		userBlocks.save(block);

		MediaAsset asset = createAndSaveMedia(bob);

		PostMedia pm = new PostMedia();
		pm.setPostId(posts.create(bob, PostType.NOTE, "blocked post", List.of()).getId());
		pm.setMediaId(asset.getId());
		pm.setPosition((short) 0);
		postMedia.save(pm);

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void avatarMediaIsVisibleToAnyMember() throws Exception {
		MediaAsset asset = createAndSaveMedia(bob);
		bob.setAvatarMediaId(asset.getId());
		users.save(bob);

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void unknownMediaIdIs404() throws Exception {
		mockMvc.perform(get("/api/v1/media/{id}/bytes", 999999L)
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void messageAttachmentIsVisibleToTheConversationMember() throws Exception {
		// The asset id deliberately lives outside the message-id range: a lookup
		// that confuses asset ids with message ids must not pass by coincidence.
		MediaAsset asset = createMediaWithExplicitId(bob, FAR_ASSET_ID);
		messaging.send(bob, alice.getId(), "look at this", List.of(asset.getId()));

		// alice2 is in the conversation but does not own the asset
		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "carol", roles = "MEMBER")
	void messageAttachmentIs404ForMembersOutsideTheConversation() throws Exception {
		CreateInviteResponse invite3 = inviteService.create(null, Role.MEMBER, admin);
		User carol = inviteService.accept(
				invite3.token(), "carol", "Carol", "carol@example.com", "carol-very-strong-password");

		MediaAsset asset = createMediaWithExplicitId(bob, FAR_ASSET_ID + 1);
		messaging.send(bob, alice.getId(), "look at this", List.of(asset.getId()));

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void messageAttachmentIs404AfterTheMemberLeftTheConversation() throws Exception {
		var group = messaging.createGroup(admin, "Team", List.of(alice.getId()));
		// the sender must own the asset — attaching someone else's media is rejected
		MediaAsset asset = createMediaWithExplicitId(admin, FAR_ASSET_ID + 2);
		messaging.sendToConversation(admin, group.getId(), "look at this", List.of(asset.getId()));

		// alice2 leaves the group — the retained membership row must not grant access
		messaging.leave(alice, group.getId());

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void purgedSenderKeepsMessageAttachmentsReadable() throws Exception {
		MediaAsset asset = createMediaWithExplicitId(bob, FAR_ASSET_ID + 3);
		Message sent = messaging.send(bob, alice.getId(), "look at this", List.of(asset.getId()));

		// a purged sender leaves messages.sender_id NULL (ON DELETE SET NULL)
		jdbc.update("UPDATE messages SET sender_id = NULL WHERE id = ?", sent.getId());

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void suspendedMemberCanStillReadMediaTheyMaySee() throws Exception {
		MediaAsset asset = createAndSaveMedia(alice);
		alice.setStatus(UserStatus.SUSPENDED);
		users.save(alice);

		// FR-1.4: suspended members may read, only writes are blocked
		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(csrf()))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "alice2", roles = "MEMBER")
	void suspendedMemberCannotUploadMedia() throws Exception {
		alice.setStatus(UserStatus.SUSPENDED);
		users.save(alice);

		mockMvc.perform(multipart("/api/v1/media")
						.file(new MockMultipartFile("file", "dot.png", "image/png", pngBytes()))
						.with(csrf()))
				.andExpect(status().isForbidden());
	}
}
