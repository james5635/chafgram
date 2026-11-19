package io.github.james5635.chafgram.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Async video pipeline: ffprobe → transcode (h264/aac/faststart) → thumbnail → blurhash → READY/FAILED.
 * Triggered AFTER_COMMIT so the media asset row exists with a stable ID.
 */
@Service
public class MediaProcessingService {

	private static final Logger log = LoggerFactory.getLogger(MediaProcessingService.class);

	private static final Duration TRANSCODE_TIMEOUT = Duration.ofMinutes(10);
	private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration THUMBNAIL_TIMEOUT = Duration.ofSeconds(30);

	/** The transcode always produces MP4 (h264/aac/faststart). */
	static final String OUTPUT_MIME_TYPE = "video/mp4";

	/** How much of a tool's output is kept for error messages. */
	private static final int MAX_CAPTURED_OUTPUT = 64 * 1024;
	private final MediaStorage storage;
	private final MediaAssetRepository mediaAssets;
	private final TaskExecutor executor;

	public MediaProcessingService(MediaStorage storage, MediaAssetRepository mediaAssets,
			@Qualifier("mediaPipelineExecutor") TaskExecutor executor) {
		this.storage = storage;
		this.mediaAssets = mediaAssets;
		this.executor = executor;
	}

	/**
	 * Entry point — called after commit via event listener. The work is handed
	 * to the bounded pipeline executor explicitly, so a full queue is visible
	 * here: the asset is marked failed instead of staying stuck in
	 * {@code processing} forever.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processAsync(Long mediaAssetId) {
		try {
			executor.execute(() -> process(mediaAssetId));
		}
		catch (TaskRejectedException e) {
			markFailed(mediaAssetId);
		}
	}

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
			markFailed(mediaAssetId);
		}
	}

	private void runPipeline(MediaAsset asset, Path source) {
		// 1) ffprobe for duration, width, height
		ProbeResult probe = ffprobe(source);
		mediaAssets.updateMeta(asset.getId(), probe.width(), probe.height(), probe.durationMs());

		// 2) transcode to h264/aac/faststart — the output is always MP4, so the
		//    stored key and MIME type must follow the new container format
		String outputKey = outputStorageKey(asset.getStorageKey());
		Path output;
		try {
			Path temp = Files.createTempFile("transcode-", ".mp4");
			try {
				transcode(source, temp);
				storage.storeProcessed(temp, outputKey);
			}
			finally {
				Files.deleteIfExists(temp);
			}
			output = storage.resolvePath(outputKey);
			long newSize = Files.size(output);
			mediaAssets.updateOutput(asset.getId(), outputKey, OUTPUT_MIME_TYPE, newSize);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to store the processed video", e);
		}

		// 3) thumbnail at 10% or 1s → blurhash (taken from the processed video)
		Path thumb = extractThumbnail(output, probe.durationMs());
		String blurhash = null;
		if (thumb != null) {
			try {
				blurhash = BlurhashEncoder.encode(ImageIO.read(thumb.toFile()), 4, 3);
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

		// 4) the source file is superseded by the transcode — drop it
		if (!outputKey.equals(asset.getStorageKey())) {
			storage.delete(asset.getStorageKey());
		}

		// 5) mark ready — only if the asset is still processing: an admin may
		//    have deleted it while the transcode ran (BR-5.3 tombstone survives)
		if (mediaAssets.markReady(asset.getId(), blurhash) == 0) {
			log.info("Media {} is no longer processing — discarding the pipeline result", asset.getId());
			return;
		}
		log.info("Pipeline completed for media {} (blurhash={})", asset.getId(), blurhash);
	}

	/**
	 * The transcode always produces MP4, so a source stored under another
	 * extension (e.g. {@code .webm}) moves to a sibling {@code .mp4} key.
	 */
	static String outputStorageKey(String sourceKey) {
		int dot = sourceKey.lastIndexOf('.');
		int slash = sourceKey.lastIndexOf('/');
		if (dot > slash && dot >= 0) {
			return sourceKey.substring(0, dot) + ".mp4";
		}
		return sourceKey + ".mp4";
	}

	// — ffmpeg / ffprobe helpers —

	record ProbeResult(int width, int height, int durationMs) {}

	/** Outcome of an external process: exit code plus whatever it printed. */
	record ProcessResult(boolean success, String output) {}

	private ProbeResult ffprobe(Path input) {
		ProcessResult result = run(new ProcessBuilder(
				"ffprobe", "-v", "error", "-print_format", "json",
				"-show_streams", "-show_format", input.toString()), PROBE_TIMEOUT);
		if (!result.success()) {
			throw new IllegalStateException("ffprobe failed: " + tail(result.output()));
		}
		return parseProbe(result.output());
	}

	static ProbeResult parseProbe(String json) {
		// Minimal JSON parsing for the fields we need
		int width = extractInt(json, "\"width\":");
		int height = extractInt(json, "\"height\":");
		int durationMs = (int) Math.round(extractDouble(json, "\"duration\":") * 1000);
		return new ProbeResult(width, height, durationMs);
	}

	private static int extractInt(String json, String key) {
		int start = valueStart(json, key);
		if (start < 0) {
			return 0;
		}
		int end = start;
		while (end < json.length() && Character.isDigit(json.charAt(end))) {
			end++;
		}
		return Integer.parseInt(json.substring(start, end));
	}

	private static double extractDouble(String json, String key) {
		int start = valueStart(json, key);
		if (start < 0) {
			return 0.0;
		}
		int end = start;
		while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
			end++;
		}
		return Double.parseDouble(json.substring(start, end));
	}

	private static int valueStart(String json, String key) {
		int index = json.indexOf(key);
		if (index < 0) {
			return -1;
		}
		index += key.length();
		while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
			index++;
		}
		if (index < json.length() && json.charAt(index) == '"') {
			index++;
		}
		return index;
	}

	private void transcode(Path input, Path output) {
		ProcessResult result = run(new ProcessBuilder(
				"ffmpeg", "-y",
				"-i", input.toString(),
				"-map_metadata", "-1",
				"-c:v", "libx264", "-crf", "23", "-preset", "veryfast",
				"-movflags", "+faststart",
				"-c:a", "aac", "-b:a", "128k",
				output.toString()), TRANSCODE_TIMEOUT);
		if (!result.success()) {
			throw new IllegalStateException("ffmpeg transcode failed: " + tail(result.output()));
		}
	}

	/**
	 * Runs an external process and drains its output while it runs.
	 * <p>
	 * ffmpeg writes progress to stdout/stderr; a pipe nobody reads fills up
	 * and blocks the process, which would look like a transcode timeout. The
	 * output is therefore read on a separate thread (bounded to the first
	 * {@value #MAX_CAPTURED_OUTPUT} characters) and the process is killed if it
	 * overruns its timeout.
	 */
	ProcessResult run(ProcessBuilder builder, Duration timeout) {
		try {
			Process proc = builder.redirectErrorStream(true).start();
			StringBuilder captured = new StringBuilder();
			Thread drain = new Thread(() -> drain(proc.getInputStream(), captured), "media-pipeline-drain");
			drain.setDaemon(true);
			drain.start();

			boolean finished = proc.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!finished) {
				proc.destroyForcibly();
				proc.waitFor(5, TimeUnit.SECONDS);
			}
			drain.join(2000);
			return new ProcessResult(finished && proc.exitValue() == 0, captured.toString());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Media processing was interrupted", e);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to run the media tool", e);
		}
	}

	/** Reads the process output into {@code sink}, keeping only the first chunk. */
	private void drain(InputStream in, StringBuilder sink) {
		try (in) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) > 0) {
				synchronized (sink) {
					if (sink.length() < MAX_CAPTURED_OUTPUT) {
						sink.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
					}
				}
			}
		}
		catch (IOException ignored) {
			// the process died — the exit code carries the real story
		}
	}

	/** The last lines of the captured output — enough for an error message. */
	private static String tail(String output) {
		if (output == null || output.isBlank()) {
			return "(no output)";
		}
		String trimmed = output.strip();
		return trimmed.length() <= 500 ? trimmed : trimmed.substring(trimmed.length() - 500);
	}

	private Path extractThumbnail(Path video, int durationMs) {
		if (durationMs <= 0) {
			return null;
		}
		int seekMs = Math.min(durationMs / 10, 1000);
		Path thumb = null;
		try {
			thumb = Files.createTempFile("thumb-", ".jpg");
			ProcessResult result = run(new ProcessBuilder(
					"ffmpeg", "-y",
					"-ss", String.valueOf(seekMs / 1000.0),
					"-i", video.toString(),
					"-frames:v", "1",
					"-vf", "scale=160:-2",
					thumb.toString()), THUMBNAIL_TIMEOUT);
			if (result.success()) {
				return thumb;
			}
			Files.deleteIfExists(thumb);
			return null;
		}
		catch (Exception e) {
			log.warn("Thumbnail extraction failed: {}", e.getMessage());
			if (thumb != null) {
				try {
					Files.deleteIfExists(thumb);
				}
				catch (IOException ignored) {}
			}
			return null;
		}
	}

	// — DB updates (minimal repo methods assumed) —

	/** Marks the asset failed so it never stays stuck in {@code processing}. */
	private void markFailed(Long id) {
		mediaAssets.findById(id).ifPresent(a -> {
			a.setStatus(MediaStatus.FAILED);
			mediaAssets.save(a);
		});
	}
}
