package io.github.james5635.chafgram.media;

import com.jayway.jsonpath.JsonPath;
import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.MemberService;
import io.github.james5635.chafgram.identity.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end video pipeline test: upload → PROCESSING → READY with duration/blurhash.
 * Requires ffmpeg/ffprobe on PATH (available in dev/CI).
 */
class MediaProcessingIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MediaAssetRepository mediaAssets;

	@Autowired
	private MemberService memberService;

	@Autowired
	private MediaStorage storage;

	private static Path testClip;

	@BeforeAll
	static void generateTestClip() {
		try {
			testClip = Files.createTempFile("test-clip-", ".mp4");
			Process proc = new ProcessBuilder(
					"ffmpeg", "-y", "-f", "lavfi",
					"-i", "testsrc=duration=2:size=320x240:rate=10",
					"-pix_fmt", "yuv420p",
					testClip.toString())
					.redirectErrorStream(true)
					.start();
			if (!proc.waitFor(30, TimeUnit.SECONDS) || proc.exitValue() != 0) {
				throw new IllegalStateException("Failed to generate test clip");
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Test clip generation failed", e);
		}
	}

	@BeforeEach
	void clean() {
		mediaAssets.deleteAll();
		wipeSocial();
		memberService.bootstrap("alice", "Alice", "alice@example.com", "a-very-strong-password");
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void videoUploadTranscodesAndProducesBlurhash() throws Exception {
		// Upload video
		var result = mockMvc.perform(multipart("/api/v1/media")
						.file(createMultipartFile("file", "clip.mp4", "video/mp4", Files.readAllBytes(testClip)))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processing"))
				.andExpect(jsonPath("$.id", notNullValue()))
				.andReturn();

		Object mediaIdValue = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		String mediaId = mediaIdValue.toString();

		// Poll for READY (up to 60s)
		MediaAsset asset = awaitReady(Long.valueOf(mediaId));

		assertThat(asset.getDurationMs()).isNotNull();
		assertThat(asset.getDurationMs()).isBetween(1500, 2500);
		assertThat(asset.getWidth()).isEqualTo(320);
		assertThat(asset.getHeight()).isEqualTo(240);
		assertThat(asset.getBlurhash()).isNotNull();
		assertThat(asset.getBlurhash().length()).isBetween(20, 64);
	}

	/** Polls the asset until the pipeline marks it ready (or fails). */
	private MediaAsset awaitReady(Long mediaId) throws Exception {
		for (int i = 0; i < 60; i++) {
			MediaAsset asset = mediaAssets.findById(mediaId).orElse(null);
			if (asset != null && "ready".equals(asset.getStatus().name().toLowerCase())) {
				return asset;
			}
			if (asset != null && "failed".equals(asset.getStatus().name().toLowerCase())) {
				throw new IllegalStateException("Pipeline marked failed");
			}
			Thread.sleep(1000);
		}
		throw new IllegalStateException("video did not reach READY within 60s");
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void webmSourceIsTranscodedToMp4AndServedWithTheMatchingMimeType() throws Exception {
		var result = mockMvc.perform(multipart("/api/v1/media")
						.file(createMultipartFile("file", "clip.webm", "video/webm", Files.readAllBytes(testClip)))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processing"))
				.andReturn();

		String mediaId = JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();
		String sourceKey = JsonPath.read(result.getResponse().getContentAsString(), "$.storageKey").toString();

		MediaAsset asset = awaitReady(Long.valueOf(mediaId));

		// the transcode produces MP4 — the key and the MIME type must say so
		assertThat(asset.getMimeType()).isEqualTo("video/mp4");
		assertThat(asset.getStorageKey()).endsWith(".mp4");
		assertThat(asset.getStorageKey()).isNotEqualTo(sourceKey);
		// the pre-transcode source is superseded and dropped
		assertThat(Files.notExists(storage.resolvePath(sourceKey))).isTrue();
		assertThat(Files.exists(storage.resolvePath(asset.getStorageKey()))).isTrue();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void corruptVideoLandsInFailed() throws Exception {
		var result = mockMvc.perform(multipart("/api/v1/media")
						.file(createMultipartFile("file", "fake.mp4", "video/mp4", "not a video".getBytes()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("processing"))
				.andReturn();

		Object mediaIdValue = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		String mediaId = mediaIdValue.toString();

		// Poll for FAILED (up to 30s)
		boolean failed = false;
		for (int i = 0; i < 30; i++) {
			var asset = mediaAssets.findById(Long.valueOf(mediaId)).orElse(null);
			if (asset != null && "failed".equals(asset.getStatus().name().toLowerCase())) {
				failed = true;
				break;
			}
			Thread.sleep(1000);
		}
		assertThat(failed).as("corrupt video did not reach FAILED within 30s").isTrue();
	}

	@Test
	@WithMockUser(username = "alice", roles = "ADMIN")
	void imageUploadInstantlyReady() throws Exception {
		byte[] png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==".getBytes();
		mockMvc.perform(multipart("/api/v1/media")
						.file(createMultipartFile("file", "dot.png", "image/png", png))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ready"))
				.andExpect(jsonPath("$.kind").value("image"));
	}

	private MockMultipartFile createMultipartFile(String name, String filename, String contentType, byte[] content) {
		return new MockMultipartFile(name, filename, contentType, content);
	}
}
