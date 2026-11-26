package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;

/**
 * Admin media moderation delete (BR-5.3): an admin can delete any media for
 * moderation; the asset is tombstoned, served nowhere, and the action lands
 * in the audit log (BR-9.2).
 */
class MediaModerationIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private MediaAssetRepository mediaAssets;

	@Autowired
	private InMemoryMediaStorage storage;

	User admin;
	User moderator;
	User member;

	@BeforeEach
	void setUp() {
		wipeSocial();
		admin = memberService.bootstrap(
				"alice", "Alice", "alice@example.com", "a-very-strong-password");
		CreateInviteResponse modInvite = inviteService.create(null, Role.MODERATOR, admin);
		moderator = inviteService.accept(modInvite.token(),
				"mod", "Mod", "mod@example.com", "mod-very-strong-password");
		CreateInviteResponse memberInvite = inviteService.create(null, Role.MEMBER, admin);
		member = inviteService.accept(memberInvite.token(),
				"bob", "Bob", "bob@example.com", "bob-very-strong-password");
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void adminCanDeleteAnyMedia() throws Exception {
		MediaAsset asset = createAndSaveMedia(member);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deleteMedia", is(true)));

		assertThat(mediaAssets.findById(asset.getId()).orElseThrow().getStatus())
				.isEqualTo(MediaStatus.DELETED);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void mediaDeleteWritesAnAuditEntry() throws Exception {
		MediaAsset asset = createAndSaveMedia(member);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk());

		Integer count = jdbc.queryForObject(
				"select count(*) from audit_log where action = 'media.delete' and target_type = 'media' and target_id = ?",
				Integer.class, asset.getId());
		assertThat(count).isEqualTo(1);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void deletedMediaIsServedToNobody() throws Exception {
		MediaAsset asset = createAndSaveMedia(member);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk());

		// The owner cannot fetch it any more …
		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.with(user("bob")))
				.andExpect(status().isNotFound());
		// … and neither can an admin.
		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId()))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void deletedMediaMetadataIsNotFound() throws Exception {
		MediaAsset asset = createAndSaveMedia(member);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ mediaAsset(id: " + asset.getId() + ") { id } }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("NOT_FOUND")));
	}

	@Test
	@WithMockUser(username = "mod", roles = "MODERATOR")
	void moderatorCannotDeleteMedia() throws Exception {
		MediaAsset asset = createAndSaveMedia(member);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")));

		assertThat(mediaAssets.findById(asset.getId()).orElseThrow().getStatus())
				.isEqualTo(MediaStatus.READY);
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void plainMemberCannotDeleteMedia() throws Exception {
		MediaAsset asset = createAndSaveMedia(member);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("FORBIDDEN")));

		assertThat(mediaAssets.findById(asset.getId()).orElseThrow().getStatus())
				.isEqualTo(MediaStatus.READY);
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void deletingUnknownMediaIsNotFound() throws Exception {
		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: 999999) }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].extensions.classification", is("NOT_FOUND")));
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void deletingTwiceIsIdempotent() throws Exception {
		MediaAsset asset = createAndSaveMedia(member);

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deleteMedia", is(true)));

		mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { deleteMedia(id: " + asset.getId() + ") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deleteMedia", is(true)));

		Integer count = jdbc.queryForObject(
				"select count(*) from audit_log where action = 'media.delete' and target_id = ?",
				Integer.class, asset.getId());
		assertThat(count).isEqualTo(1);
	}

	/** A ready image asset owned by {@code owner}, with bytes on disk. */
	private MediaAsset createAndSaveMedia(User owner) {
		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(owner.getId());
		asset.setKind(MediaKind.IMAGE);
		asset.setStatus(MediaStatus.READY);
		String storageKey = "test-media/" + System.nanoTime() + ".png";
		asset.setStorageKey(storageKey);
		asset.setMimeType("image/png");
		asset.setSizeBytes(100L);
		storage.put(storageKey, new byte[] { 1, 2, 3, 4 });
		return mediaAssets.save(asset);
	}
}
