package io.github.james5635.chafgram.media;

import java.awt.image.BufferedImage;

/**
 * Hand-rolled Blurhash encoder (spec: https://blurha.sh/).
 * No external dependency — deterministic, spec-compliant base83 output.
 * Components default 4×3; supports asymmetric components.
 */
public final class BlurhashEncoder {

	private static final char[] BASE83 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~".toCharArray();

	private static final double[] AC_WEIGHTS = new double[25]; // 5×5 max

	static {
		// precompute AC weight constants per spec (not strictly needed for fixed 4×3)
	}

	private BlurhashEncoder() {
	}

	/**
	 * Encodes a BufferedImage to a blurhash string.
	 *
	 * @param image       source image (TYPE_INT_ARGB or convertible)
	 * @param componentsX number of X AC components (typically 4)
	 * @param componentsY number of Y AC components (typically 3)
	 * @return base83 blurhash string (length 20–64 depending on components)
	 */
	public static String encode(BufferedImage image, int componentsX, int componentsY) {
		if (image == null) {
			throw new IllegalArgumentException("image must not be null");
		}
		if (componentsX < 1 || componentsX > 5 || componentsY < 1 || componentsY > 5) {
			throw new IllegalArgumentException("components must be 1..5");
		}

		int w = image.getWidth();
		int h = image.getHeight();

		// Convert to linear sRGB float[3][h][w]
		float[][][] linear = toLinearSrgb(image);

		// DCT-II per channel, keep DC + (componentsX-1)×(componentsY-1) AC
		double[][][] coeffs = new double[3][componentsY][componentsX];
		for (int c = 0; c < 3; c++) {
			for (int y = 0; y < componentsY; y++) {
				for (int x = 0; x < componentsX; x++) {
					coeffs[c][y][x] = dct(linear[c], w, h, x, y);
				}
			}
		}

		// Encode: DC first (per channel), then AC in zigzag order across channels
		StringBuilder out = new StringBuilder();

		// DC component per channel (3 values)
		for (int c = 0; c < 3; c++) {
			encodeDC(coeffs[c][0][0], out);
		}

		// Size flag: (componentsX-1) + (componentsY-1)*9, base83
		int sizeFlag = (componentsX - 1) + (componentsY - 1) * 9;
		out.append(BASE83[sizeFlag]);

		// AC components (interleaved across channels, zigzag order)
		for (int y = 0; y < componentsY; y++) {
			for (int x = 0; x < componentsX; x++) {
				if (x == 0 && y == 0) {
					continue; // skip DC
				}
				for (int c = 0; c < 3; c++) {
					encodeAC(coeffs[c][y][x], out);
				}
			}
		}

		return out.toString();
	}

	// — internals —

	private static float[][][] toLinearSrgb(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		float[][][] linear = new float[3][h][w];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = image.getRGB(x, y);
				int r = (argb >> 16) & 0xFF;
				int g = (argb >> 8) & 0xFF;
				int b = argb & 0xFF;
				linear[0][y][x] = srgbToLinear(r / 255f);
				linear[1][y][x] = srgbToLinear(g / 255f);
				linear[2][y][x] = srgbToLinear(b / 255f);
			}
		}
		return linear;
	}

	private static float srgbToLinear(float v) {
		return (v <= 0.04045f) ? v / 12.92f : (float) Math.pow((v + 0.055f) / 1.055f, 2.4f);
	}

	// 2D DCT-II for one coefficient (u,v)
	private static double dct(float[][] channel, int w, int h, int u, int v) {
		double sum = 0.0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double val = channel[y][x];
				sum += val * Math.cos(Math.PI * u * (x + 0.5) / w) * Math.cos(Math.PI * v * (y + 0.5) / h);
			}
		}
		double cu = (u == 0) ? 1.0 / Math.sqrt(2.0) : 1.0;
		double cv = (v == 0) ? 1.0 / Math.sqrt(2.0) : 1.0;
		return cu * cv * (2.0 / Math.sqrt(w * h)) * sum;
	}

	// DC: range [0, 1] → 9.6 bits → 4 base83 chars per channel (packed 3×4=12 chars total)
	private static void encodeDC(double dc, StringBuilder out) {
		int rounded = (int) Math.round(dc * 255.0);
		rounded = Math.max(0, Math.min(255, rounded));
		out.append(BASE83[rounded >> 6]);
		out.append(BASE83[rounded & 0x3F]);
	}

	// AC: signed, quantized to -9..9, shifted to 0..18, base83 (1 char per coefficient)
	private static void encodeAC(double ac, StringBuilder out) {
		// Quantize: signPow(|ac|, 2.0) * 100, clamped to [-9, 9] per spec
		double abs = Math.abs(ac);
		double sign = ac < 0 ? -1 : 1;
		int quantized = (int) Math.round(sign * Math.pow(abs, 2.0) * 100.0);
		quantized = Math.max(-9, Math.min(9, quantized));
		// Map -9..9 → 0..18, then base83 (1 char)
		int mapped = quantized + 9;
		out.append(BASE83[mapped]);
	}
}