package io.github.james5635.chafgram.media;

import io.github.james5635.chafgram.AbstractIntegrationTest;
import io.github.james5635.chafgram.identity.MemberService;
import io.github.james5635.chafgram.identity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** `media_assets` lifecycle transitions (FR-5.2, BR-5.3). */
class MediaAssetRepositoryTest extends AbstractIntegrationTest {

	@Autowired
	private MediaAssetRepository mediaAssets;

	@Autowired
	private MemberService memberService;

	private User owner;

	@BeforeEach
	void setUp() {
		wipeSocial();
		owner = memberService.bootstrap("alice", "Alice", "alice@example.com", "a-very-strong-password");
	}

	@Test
	void markReadyTransitionsAProcessingAssetToReady() {
		MediaAsset asset = videoAsset("ready.mp4", MediaStatus.PROCESSING);
		mediaAssets.save(asset);

		mediaAssets.markReady(asset.getId(), "hash");

		assertThat(mediaAssets.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(MediaStatus.READY);
	}

	/**
	 * FR-5.2/BR-5.3: a moderation delete (status='deleted') must survive the
	 * pipeline — the byte authorizer could otherwise serve the asset again.
	 */
	@Test
	void markReadyDoesNotResurrectAModeratedAsset() {
		MediaAsset asset = videoAsset("deleted.mp4", MediaStatus.DELETED);
		mediaAssets.save(asset);

		mediaAssets.markReady(asset.getId(), "hash");

		assertThat(mediaAssets.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(MediaStatus.DELETED);
	}

	@Test
	void markFailedDoesNotFlipAModeratedAsset() {
		MediaAsset asset = videoAsset("deleted-fail.mp4", MediaStatus.DELETED);
		mediaAssets.save(asset);

		mediaAssets.markFailed(asset.getId());

		assertThat(mediaAssets.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(MediaStatus.DELETED);
	}

	private MediaAsset videoAsset(String key, MediaStatus status) {
		MediaAsset asset = new MediaAsset();
		asset.setOwnerUserId(owner.getId());
		asset.setKind(MediaKind.VIDEO);
		asset.setStatus(status);
		asset.setStorageKey("test/" + key);
		asset.setMimeType("video/mp4");
		asset.setSizeBytes(1000L);
		return asset;
	}
}
