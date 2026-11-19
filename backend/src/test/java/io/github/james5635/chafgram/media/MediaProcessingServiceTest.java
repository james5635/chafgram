package io.github.james5635.chafgram.media;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

	/**
	 * A bounded pipeline executor rejects work once its queue is full. The
	 * asset row is already committed at that point, so it must not stay stuck
	 * in {@code processing} — it is marked failed instead.
	 */
	@Test
	void queueRejectionMarksTheAssetFailedInsteadOfLeavingItProcessing() {
		MediaAssetRepository assets = mock(MediaAssetRepository.class);
		when(assets.markFailed(42L)).thenReturn(1);

		MediaProcessingService service = new MediaProcessingService(
				mock(MediaStorage.class), assets, task -> {
					throw new TaskRejectedException("pipeline queue is full");
				});

		service.processAsync(42L);

		verify(assets).markFailed(42L);
	}

	/**
	 * ffmpeg writes progress to stdout/stderr. A pipe nobody reads fills up
	 * and blocks the process — the transcode then looks like a timeout. The
	 * runner must therefore drain the output while the process runs.
	 */
	@Test
	void subprocessOutputIsDrainedSoTheProcessCannotBlockOnAFullPipe() {
		MediaProcessingService service = new MediaProcessingService(
				mock(MediaStorage.class), mock(MediaAssetRepository.class), Runnable::run);

		// writes far more than a pipe buffer holds (200 kB)
		var result = service.run(
				new ProcessBuilder("sh", "-c", "yes chafgram | head -c 200000"),
				Duration.ofSeconds(30));

		assertThat(result.success())
				.as("the process must finish instead of blocking on an unread pipe")
				.isTrue();
		assertThat(result.output()).isNotEmpty();
	}
}
