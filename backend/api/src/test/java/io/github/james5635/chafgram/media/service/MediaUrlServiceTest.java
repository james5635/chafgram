package io.github.james5635.chafgram.media.service;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.config.JwtConfig;
import io.github.james5635.chafgram.identity.User;
import io.github.james5635.chafgram.identity.auth.TokenService;
import io.github.james5635.chafgram.identity.repository.UserRepository;
import io.github.james5635.chafgram.media.InMemoryMediaStorage;
import io.github.james5635.chafgram.media.MediaAsset;
import io.github.james5635.chafgram.media.MediaKind;
import io.github.james5635.chafgram.media.MediaStatus;
import io.github.james5635.chafgram.media.repository.MediaAssetRepository;
import io.github.james5635.chafgram.social.Post;
import io.github.james5635.chafgram.social.PostType;
import io.github.james5635.chafgram.social.dto.PostDto;
import io.github.james5635.chafgram.social.dto.Views;
import io.github.james5635.chafgram.social.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Short-lived signed media URLs (Task 4). The browser cannot carry a bearer
 * header on {@code <img>}/{@code <video>}, so a signed URL is bound to one
 * viewer and cannot be replayed after its expiry or by another member.
 */
class MediaUrlServiceTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MediaUrlService mediaUrls;

	@Autowired
	private JwtConfig jwtConfig;

	@Autowired
	private UserRepository users;

	@Autowired
	private MediaAssetRepository mediaAssets;

	@Autowired
	private InMemoryMediaStorage storage;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private Views views;

	@Autowired
	private PostService posts;

	User alice;
	User bob;

	@BeforeEach
	void setUp() {
		wipeSocial();
		jdbc.update("DELETE FROM refresh_tokens");
		alice = insertMember("alice", "Alice");
		bob = insertMember("bob", "Bob");
	}

	@Test
	void signUsesAOneHourTtlAndBindsIdExpiryAndViewer() throws Exception {
		long before = Instant.now().getEpochSecond();

		String url = mediaUrls.sign(41L, alice.getId());
		Map<String, String> query = query(url);
		long exp = Long.parseLong(query.get("exp"));

		assertThat(url).startsWith("/api/media/41?");
		assertThat(exp).isBetween(before + 3595, before + 3665);
		assertThat(query.get("uid")).isEqualTo(String.valueOf(alice.getId()));
		assertThat(query.get("sig")).isEqualTo(expectedSig(41L, exp, alice.getId()));
	}

	@Test
	void validationRejectsTamperedExpiredAndWrongViewerUrls() throws Exception {
		String url = mediaUrls.sign(41L, alice.getId());
		Map<String, String> query = query(url);
		String exp = query.get("exp");
		String sig = query.get("sig");
		String aliceId = String.valueOf(alice.getId());

		assertThat(mediaUrls.validate(41L, exp, aliceId, sig)).isEqualTo(alice.getId());

		assertThatThrownBy(() -> mediaUrls.validate(41L, exp, aliceId, "0" + sig))
				.isInstanceOf(RuntimeException.class);
		long expired = Instant.now().getEpochSecond() - 10;
		assertThatThrownBy(() -> mediaUrls.validate(41L, String.valueOf(expired), aliceId,
				expectedSig(41L, expired, alice.getId())))
				.isInstanceOf(RuntimeException.class);
		assertThatThrownBy(() -> mediaUrls.validate(41L, exp, String.valueOf(bob.getId()), sig))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	void signedBytesUrlStreamsWithPrivateShortCache() throws Exception {
		MediaAsset asset = createAsset(alice);

		mockMvc.perform(get(backendBytesPath(mediaUrls.sign(asset.getId(), alice.getId()))))
				.andExpect(status().isOk())
				.andExpect(content().bytes(new byte[]{1, 2, 3, 4}))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, max-age=300"));
	}

	@Test
	void tamperedExpiredAndWrongViewerBytesUrlsAreRejected() throws Exception {
		MediaAsset asset = createAsset(alice);
		Map<String, String> query = query(mediaUrls.sign(asset.getId(), alice.getId()));
		String aliceId = String.valueOf(alice.getId());
		long expired = Instant.now().getEpochSecond() - 10;

		List<String> badUrls = List.of(
				signedUrl(asset.getId(), query.get("exp"), aliceId, "0" + query.get("sig")),
				signedUrl(asset.getId(), String.valueOf(expired), aliceId,
						expectedSig(asset.getId(), expired, alice.getId())),
				signedUrl(asset.getId(), query.get("exp"), String.valueOf(bob.getId()), query.get("sig")));

		for (String badUrl : badUrls) {
			mockMvc.perform(get(backendBytesPath(badUrl)))
					.andExpect(status().isUnauthorized());
		}
	}

	@Test
	void postViewsCarrySignedUrlsBoundToTheViewer() {
		MediaAsset asset = createAsset(alice);
		Post post = posts.create(alice, PostType.PHOTO, "a photo", List.of(asset.getId()));

		PostDto dto = views.post(post, alice);

		assertThat(dto.mediaIds()).containsExactly(asset.getId());
		assertThat(dto.mediaUrls()).hasSize(1);
		Map<String, String> query = query(dto.mediaUrls().get(0));
		assertThat(mediaUrls.validate(asset.getId(), query.get("exp"), query.get("uid"), query.get("sig")))
				.isEqualTo(alice.getId());
	}

	@Test
	void aValidSignedUrlCannotBeReplayedByAnotherBearer() throws Exception {
		MediaAsset asset = createAsset(alice);

		mockMvc.perform(get(backendBytesPath(mediaUrls.sign(asset.getId(), alice.getId())))
						.header(AUTHORIZATION, "Bearer " + tokenService.issue(bob).accessToken()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void signedUrlEndpointRequiresBearerAndReturnsAFreshUrl() throws Exception {
		MediaAsset asset = createAsset(alice);

		mockMvc.perform(get("/api/v1/media/{id}/signed-url", asset.getId()))
				.andExpect(status().isUnauthorized());

		MvcResult result = mockMvc.perform(get("/api/v1/media/{id}/signed-url", asset.getId())
						.header(AUTHORIZATION, "Bearer " + tokenService.issue(alice).accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url", containsString("/api/media/" + asset.getId() + "?exp=")))
				.andExpect(jsonPath("$.expiresAt").exists())
				.andReturn();

		Matcher matcher = Pattern.compile("\"url\":\"([^\"]+)\"")
				.matcher(result.getResponse().getContentAsString());
		assertThat(matcher.find()).isTrue();
		String url = matcher.group(1).replace("\\/", "/");
		Map<String, String> query = query(url);
		assertThat(mediaUrls.validate(asset.getId(), query.get("exp"), query.get("uid"), query.get("sig")))
				.isEqualTo(alice.getId());
	}

	@Test
	void signedUrlEndpointDeniesMediaTheViewerCannotRead() throws Exception {
		MediaAsset asset = createAsset(bob);

		mockMvc.perform(get("/api/v1/media/{id}/signed-url", asset.getId())
						.header(AUTHORIZATION, "Bearer " + tokenService.issue(alice).accessToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void bearerBytesPathStillWorksForApiClients() throws Exception {
		MediaAsset asset = createAsset(alice);

		mockMvc.perform(get("/api/v1/media/{id}/bytes", asset.getId())
						.header(AUTHORIZATION, "Bearer " + tokenService.issue(alice).accessToken()))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, max-age=300"));
	}

	private User insertMember(String username, String displayName) {
		jdbc.update("INSERT INTO users (username, display_name, email, role, status) "
				+ "VALUES (?, ?, ?, 'member', 'active')",
				username, displayName, username + "@example.com");
		return users.findByUsername(username).orElseThrow();
	}

	private MediaAsset createAsset(User owner) {
		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(owner.getId());
		asset.setKind(MediaKind.IMAGE);
		asset.setStatus(MediaStatus.READY);
		String key = "signed-media/" + System.nanoTime() + ".png";
		asset.setStorageKey(key);
		asset.setMimeType("image/png");
		asset.setSizeBytes(4L);
		storage.put(key, new byte[]{1, 2, 3, 4});
		return mediaAssets.save(asset);
	}

	/** The browser uses the BFF path; the backend validates the same query on its bytes route. */
	private String backendBytesPath(String signedUrl) {
		return signedUrl.replaceFirst("^/api/media/(\\d+)", "/api/v1/media/$1/bytes");
	}

	private String signedUrl(long mediaId, String exp, String uid, String sig) {
		return "/api/media/" + mediaId + "?exp=" + exp + "&uid=" + uid + "&sig=" + sig;
	}

	private Map<String, String> query(String url) {
		try {
			String raw = new URI(url).getQuery();
			return Arrays.stream(raw.split("&"))
					.map(part -> part.split("=", 2))
					.collect(Collectors.toMap(part -> part[0], part -> part[1]));
		}
		catch (URISyntaxException e) {
			throw new AssertionError("Malformed signed URL: " + url, e);
		}
	}

	/** Compute the HMAC independently of the service under test. */
	private String expectedSig(long mediaId, long exp, long viewerId) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(jwtConfig.signingSecret(), "HmacSHA256"));
		byte[] digest = mac.doFinal(
				(mediaId + ":" + exp + ":" + viewerId).getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(digest);
	}
}
