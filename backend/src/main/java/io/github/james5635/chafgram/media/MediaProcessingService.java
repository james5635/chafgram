package io.github.james5635.chafgram.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Async video pipeline: ffprobe → transcode (h264/aac/faststart) → thumbnail → blurhash → READY/FAILED.
 * Triggered AFTER_COMMIT so the media asset row exists with a stable ID.
 */
@Service
public class MediaProcessingService {

	private static final Logger log = LoggerFactory.getLogger(MediaProcessingService.class);

	private static final Duration TRANSCODE_TIMEOUT = Duration.ofMinutes(10);
	private final MediaStorage storage;
	private final MediaAssetRepository mediaAssets;

	public MediaProcessingService(MediaStorage storage, MediaAssetRepository mediaAssets) {
		this.storage = storage;
		this.mediaAssets = mediaAssets;
	}

	/** Entry point — called after commit via event listener. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processAsync(Long mediaAssetId) {
		process(mediaAssetId);
	}

	@Async("mediaPipelineExecutor")
	public void process(Long mediaAssetId) {
		MediaAsset asset = mediaAssets.findById(mediaAssetId).orElse(null);
		if (asset == null || asset.getKind() != MediaKind.VIDEO) {
			return; // only videos go through the pipeline
		}
		try {
			Path stored = storage.resolvePath(asset.getStorageKey());
			runPipeline(asset, stored);
		}
		catch (Exception e) {
			log.error("Pipeline failed for media {}", mediaAssetId, e);
			markFailed(mediaAssetId, e.getMessage());
		}
	}

	private void runPipeline(MediaAsset asset, Path source) {
		// 1) ffprobe for duration, width, height
		ProbeResult probe = ffprobe(source);
		mediaAssets.updateMeta(asset.getId(), probe.width(), probe.height(), probe.durationMs());

		// 2) transcode to h264/aac/faststart, overwrite same storage key
		Path temp;
		try {
			temp = Files.createTempFile("transcode-", ".mp4");
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to create temp file", e);
		}
		try {
			transcode(source, temp);
			storage.storeProcessed(temp, asset.getStorageKey());
			long newSize = Files.size(storage.resolvePath(asset.getStorageKey()));
			mediaAssets.updateSize(asset.getId(), newSize);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read processed file size", e);
		}
		finally {
			try {
				Files.deleteIfExists(temp);
			}
			catch (IOException ignored) {}
		}

		// 3) thumbnail at 10% or 1s → blurhash
		Path thumb = extractThumbnail(source, probe.durationMs());
		String blurhash = null;
		if (thumb != null) {
			try {
				blurhash = BlurhashEncoder.encode(javax.imageio.ImageIO.read(thumb.toFile()), 4, 3);
			}
			catch (Exception e) {
				log.warn("Blurhash failed for media {}: {}", asset.getId(), e.getMessage());
			}
			finally {
				try {
					Files.deleteIfExists(thumb);
				}
				catch (IOException ignored) {}
			}
		}

		// 4) mark ready
		mediaAssets.markReady(asset.getId(), blurhash);
		log.info("Pipeline completed for media {} (blurhash={})", asset.getId(), blurhash);
	}

	// — ffmpeg / ffprobe helpers —

	private record ProbeResult(int width, int height, int durationMs) {}

	private ProbeResult ffprobe(Path input) {
		try {
			Process proc = new ProcessBuilder(
					"ffprobe", "-v", "error", "-print_format", "json",
					"-show_streams", "-show_format", input.toString())
					.redirectErrorStream(true)
					.start();
			String json = new String(proc.getInputStream().readAllBytes());
			if (proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS) && proc.exitValue() == 0) {
				return parseProbe(json);
			}
			throw new IllegalStateException("ffprobe failed: " + json);
		}
		catch (Exception e) {
			throw new IllegalStateException("ffprobe error", e);
		}
	}

	private ProbeResult parseProbe(String json) {
		// Minimal JSON parsing for the fields we need
		int width = extractInt(json, "\"width\":");
		int height = extractInt(json, "\"height\":");
		int durationMs = (int) Math.round(extractDouble(json, "\"duration\":") * 1000);
		return new ProbeResult(width, height, durationMs);
	}

	private static int extractInt(String json, String key) {
		int i = json.indexOf(key);
		if (i < 0) {
			return 0;
		}
		i += key.length();
		int end = i;
		while (end < json.length() && Character.isDigit(json.charAt(end))) {
			end++;
		}
		return Integer.parseInt(json.substring(i, end));
	}

	private static double extractDouble(String json, String key) {
		int i = json.indexOf(key);
		if (i < 0) {
			return 0.0;
		}
		i += key.length();
		int end = i;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
			end++;
		}
		return Double.parseDouble(json.substring(i, end));
	}

	private void transcode(Path input, Path output) {
		try {
			Process proc = new ProcessBuilder(
					"ffmpeg", "-y",
					"-i", input.toString(),
					"-map_metadata", "-1",
					"-c:v", "libx264", "-crf", "23", "-preset", "veryfast",
					"-movflags", "+faststart",
					"-c:a", "aac", "-b:a", "128k",
					output.toString())
					.redirectErrorStream(true)
					.start();
			if (!proc.waitFor(10, java.util.concurrent.TimeUnit.MINUTES) || proc.exitValue() != 0) {
				throw new IllegalStateException("ffmpeg transcode timeout or failed");
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("transcode error", e);
		}
	}

	private Path extractThumbnail(Path video, int durationMs) {
		if (durationMs <= 0) {
			return null;
		}
		int seekMs = Math.min(durationMs / 10, 1000);
		try {
			Path thumb = Files.createTempFile("thumb-", ".jpg");
			Process proc = new ProcessBuilder(
					"ffmpeg", "-y",
					"-ss", String.valueOf(seekMs / 1000.0),
					"-i", video.toString(),
					"-frames:v", "1",
					"-vf", "scale=160:-2",
					thumb.toString())
					.redirectErrorStream(true)
					.start();
			if (proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS) && proc.exitValue() == 0) {
				return thumb;
			}
			return null;
		}
		catch (Exception e) {
			log.warn("Thumbnail extraction failed: {}", e.getMessage());
			return null;
		}
	}

	// — DB updates (minimal repo methods assumed) —

	private void markFailed(Long id, String reason) {
		mediaAssets.findById(id).ifPresent(a -> {
			a.setStatus(MediaStatus.FAILED);
			mediaAssets.save(a);
		});
	}
}