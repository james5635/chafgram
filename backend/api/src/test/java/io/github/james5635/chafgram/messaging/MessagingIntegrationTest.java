package io.github.james5635.chafgram.messaging;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.service.InviteService;
import io.github.james5635.chafgram.identity.service.MemberService;
import io.github.james5635.chafgram.identity.Role;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.dto.CreateInviteResponse;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.MediaStatus;
import io.github.james5635.chafgram.social.utils.CurrentMember;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import io.github.james5635.chafgram.messaging.service.MessagingService;

/**
 * End-to-end media messaging tests (FR-5.4, FR-6.6).
 */
@AutoConfigureMockMvc
class MessagingIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MemberService memberService;

	@Autowired
	private InviteService inviteService;

	@Autowired
	private MediaAssetRepository mediaAssets;

	@Autowired
	private CurrentMember current;

	@Autowired
	private MessagingService messaging;

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
	@WithMockUser(username = "alice", roles = "ADMIN")
	void directMessageWithImageMedia() throws Exception {
		MediaAsset img = imageAsset(admin);
		mediaAssets.save(img);

		var result = mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo.png", "image/png", pngBytes()))
						.param("userId", member.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type", is("image")))
				.andExpect(jsonPath("$.media", hasSize(1)))
				.andExpect(jsonPath("$.media[0].id").exists())
				.andReturn();

		Long mediaId = jsonPathExtract(result, "$.media[0].id");
		assertThat(mediaId).isNotNull();
	}

	/**
	 * FR-5.2: the inline message upload path must carry the same image
	 * metadata as POST /api/v1/media — dimensions and blurhash.
	 */
	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void inlineImageUploadPopulatesDimensionsAndBlurhash() throws Exception {
		mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo.png", "image/png", realPngBytes()))
						.param("userId", member.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.media[0].width", is(1)))
				.andExpect(jsonPath("$.media[0].height", is(1)))
				.andExpect(jsonPath("$.media[0].blurhash", notNullValue()));
	}

	/** A real 1×1 PNG (the other fixtures pass base64 text as bytes). */
	private byte[] realPngBytes() {
		return Base64.getDecoder()
				.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==");
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void directMessageWithReadyVideoAttachedById() throws Exception {
		// Videos travel the async flow: upload → ready → attach by id
		MediaAsset vid = videoAsset(admin);
		mediaAssets.save(vid);

		var result = mockMvc.perform(multipart("/api/v1/messages")
						.param("userId", member.getId().toString())
						.param("mediaIds", vid.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type", is("video")))
				.andExpect(jsonPath("$.media", hasSize(1)))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void inlineVideoUploadIsRejectedWithTheAsyncFlowHint() throws Exception {
		// A video cannot be ready in the same request (FR-5.2) — nothing is stored
		mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "clip.mp4", "video/mp4", "fake video".getBytes(StandardCharsets.UTF_8)))
						.param("userId", member.getId().toString())
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", containsString("POST /api/v1/media")));

		assertThat(mediaAssets.count()).isZero();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void groupMessageWithMedia() throws Exception {
		MediaAsset img = imageAsset(admin);
		mediaAssets.save(img);

		// Create group
		var createResult = mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"mutation { createGroupConversation(title: \\\"Test Group\\\", memberIds: [\\\"" + admin.getId() + "\\\"]) { id } }\"}"))
				.andExpect(status().isOk())
				.andReturn();
		Long groupId = jsonPathExtract(createResult, "$.data.createGroupConversation.id");

		// Send message with media to group
		var result = mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo.png", "image/png", pngBytes()))
						.param("conversationId", groupId.toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type", is("image")))
				.andExpect(jsonPath("$.media", hasSize(1)))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void messageWithMultipleMedia() throws Exception {
		MediaAsset img1 = imageAsset(admin);
		MediaAsset img2 = imageAsset(admin);
		mediaAssets.save(img1);
		mediaAssets.save(img2);

		var result = mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo1.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo2.png", "image/png", pngBytes()))
						.param("userId", member.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.media", hasSize(2)))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void messageBodyIsOptionalWhenMediaPresent() throws Exception {
		MediaAsset img = imageAsset(admin);
		mediaAssets.save(img);

		var result = mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo.png", "image/png", pngBytes()))
						.param("userId", member.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body", nullValue()))
				.andExpect(jsonPath("$.type", is("image")))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void directMessageWithAnotherMembersMediaFails() throws Exception {
		// Create carol via invite
		CreateInviteResponse invite = inviteService.create(null, Role.MEMBER, admin);
		User carol = inviteService.accept(
				invite.token(), "carol", "Carol", "carol@example.com", "carol-very-strong-password");
		MediaAsset carolsImage = imageAsset(carol);
		mediaAssets.save(carolsImage);

		// alice tries to use carol's media
		var result = mockMvc.perform(multipart("/api/v1/messages")
						.param("userId", member.getId().toString())
						.param("mediaIds", carolsImage.getId().toString())
						.with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail", is("Media belongs to another member")))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void messageWithProcessingVideoFails() throws Exception {
		MediaAsset processingVid = videoAsset(admin);
		processingVid.setStatus(MediaStatus.PROCESSING);
		mediaAssets.save(processingVid);

		var result = mockMvc.perform(multipart("/api/v1/messages")
						.param("userId", member.getId().toString())
						.param("mediaIds", processingVid.getId().toString())
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("Media is not ready yet")))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "bob", roles = "MEMBER")
	void unreadMessageCountIsExposedForAConversation() throws Exception {
		var conversation = messaging.send(admin, member.getId(), "ping", List.of());

		var result = mockMvc.perform(post("/graphql")
						.with(csrf())
						.contentType("application/json")
						.content("{\"query\":\"{ unreadMessageCount(conversationId: \\\""
								+ conversation.getConversationId() + "\\\") }\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.unreadMessageCount", is(1)))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void messageWithMoreThan10AttachmentsFails() throws Exception {
		// Create 11 images
		for (int i = 0; i < 11; i++) {
			MediaAsset img = imageAsset(admin);
			mediaAssets.save(img);
		}

		var result = mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo0.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo1.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo2.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo3.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo4.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo5.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo6.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo7.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo8.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo9.png", "image/png", pngBytes()))
						.file(createMultipartFile("file", "photo10.png", "image/png", pngBytes()))
						.param("userId", member.getId().toString())
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail", is("At most 10 media items can be attached")))
				.andReturn();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void messageTypeDerivedFromAttachments() throws Exception {
		// image only -> type IMAGE
		MediaAsset img = imageAsset(admin);
		mediaAssets.save(img);

		var imgResult = mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo.png", "image/png", pngBytes()))
						.param("userId", member.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type", is("image")))
				.andReturn();

		// video -> type VIDEO
		MediaAsset vid = videoAsset(admin);
		mediaAssets.save(vid);

		var vidResult = mockMvc.perform(multipart("/api/v1/messages")
						.param("userId", member.getId().toString())
						.param("mediaIds", vid.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type", is("video")))
				.andReturn();

		// mixed -> type VIDEO
		MediaAsset img2 = imageAsset(admin);
		MediaAsset vid2 = videoAsset(admin);
		mediaAssets.save(img2);
		mediaAssets.save(vid2);

		var mixedResult = mockMvc.perform(multipart("/api/v1/messages")
						.file(createMultipartFile("file", "photo.png", "image/png", pngBytes()))
						.param("userId", member.getId().toString())
						.param("mediaIds", vid2.getId().toString())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type", is("video")))
				.andReturn();
	}

	// — helpers —

	private MediaAsset imageAsset(User owner) {
		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(owner.getId());
		asset.setKind(MediaKind.IMAGE);
		asset.setStatus(MediaStatus.READY);
		asset.setStorageKey("test/" + System.nanoTime() + ".png");
		asset.setMimeType("image/png");
		asset.setWidth(640);
		asset.setHeight(480);
		asset.setSizeBytes(1000L);
		return asset;
	}

	private MediaAsset videoAsset(User owner) {
		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(owner.getId());
		asset.setKind(MediaKind.VIDEO);
		asset.setStatus(MediaStatus.READY);
		asset.setStorageKey("test/" + System.nanoTime() + ".mp4");
		asset.setMimeType("video/mp4");
		asset.setDurationMs(5000);
		asset.setWidth(640);
		asset.setHeight(480);
		asset.setSizeBytes(10000L);
		return asset;
	}

	private byte[] pngBytes() {
		return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==".getBytes(StandardCharsets.UTF_8);
	}

	private MockMultipartFile createMultipartFile(String name, String filename, String contentType, byte[] content) {
		return new MockMultipartFile(name, filename, contentType, content);
	}

	private Long jsonPathExtract(MvcResult result, String path) {
		try {
			return Long.parseLong(JsonPath.read(result.getResponse().getContentAsString(), path).toString());
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}