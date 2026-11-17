package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.common.ApiException;
import io.github.james5635.chafgram.identity.InviteService;
import io.github.james5635.chafgram.identity.MemberService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.UserRepository;
import io.github.james5635.chafgram.identity.WorkspaceSettingsRepository;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.social.PostMedia;
import io.github.james5635.chafgram.social.PostMediaRepository;
import io.github.james5635.chafgram.social.PostService;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostRepository;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.UserBlock;
import io.github.james5635.chafgram.social.UserBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Media bytes authorization tests (FR-5.4, BR-2.2, BR-3.1, BR-5.2).
 * All deny paths return identical 404 — no information leakage.
 */
@AutoConfigureMockMvc
class MediaAccessIntegrationTest extends AbstractIntegrationTest {

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
	private PostMediaRepository postMedia;

	@Autowired
	private UserRepository users;

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

		// Create actual file on disk
		try {
			Path filePath = Paths.get("media-data", storageKey);
			Files.createDirectories(filePath.getParent());
			Files.write(filePath, new byte[]{1, 2, 3, 4});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return mediaAssets.save(asset);
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
}
