package io.github.james5635.chafgram.media;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaProcessingServiceTest {

	@Test
	void parsesProbeNumbersWhenJsonHasWhitespaceAfterColons() {
		var result = MediaProcessingService.parseProbe("""
				{"streams":[{"width": 320,"height": 240}],"format":{"duration": "2.000000"}}
				""");

		assertThat(result.width()).isEqualTo(320);
		assertThat(result.height()).isEqualTo(240);
		assertThat(result.durationMs()).isEqualTo(2000);
	}
}
