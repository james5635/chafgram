package io.github.james5635.chafgram.media;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.github.james5635.chafgram.media.utils.BlurhashEncoder;

/** Determinism + contract tests for the blurhash encoder (no decode, spec-only). */
class BlurhashEncoderTest {

	@Test
	void deterministicSamePixelsProduceSameHash() {
		BufferedImage img = colorImage(16, 9, 0xFF0000FF); // red
		String h1 = BlurhashEncoder.encode(img, 4, 3);
		String h2 = BlurhashEncoder.encode(img, 4, 3);
		assertThat(h1).isEqualTo(h2);
	}

	@Test
	void differentImagesProduceDifferentHashes() {
		BufferedImage red = colorImage(16, 9, 0xFF0000FF);
		BufferedImage blue = colorImage(16, 9, 0x0000FFFF);
		assertThat(BlurhashEncoder.encode(red, 4, 3)).isNotEqualTo(BlurhashEncoder.encode(blue, 4, 3));
	}

	@Test
	void hashLengthInValidRange() {
		BufferedImage img = colorImage(32, 24, 0xFFFFFFFF);
		String h = BlurhashEncoder.encode(img, 4, 3);
		assertThat(h.length()).isBetween(20, 64);
	}

	@Test
	void hashCharsetIsBase83() {
		BufferedImage img = colorImage(16, 9, 0xFF0000FF);
		String h = BlurhashEncoder.encode(img, 4, 3);
		assertThat(h).matches("[0-9A-Za-z#$%*+,-.:;=?@\\[\\]^_{|}~]+");
	}

	@Test
	void oneByOneImageDoesNotThrow() {
		BufferedImage img = colorImage(1, 1, 0x808080FF);
		// should not throw even for 1×1 images
		assertThatCode(() -> BlurhashEncoder.encode(img, 4, 3)).doesNotThrowAnyException();
	}

	@Test
	void asymmetricComponentsAllowed() {
		BufferedImage img = colorImage(16, 9, 0xFF0000FF);
		String h = BlurhashEncoder.encode(img, 5, 2); // wider than tall
		assertThat(h).isNotEmpty();
	}

	private BufferedImage colorImage(int w, int h, int argb) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				img.setRGB(x, y, argb);
			}
		}
		return img;
	}
}