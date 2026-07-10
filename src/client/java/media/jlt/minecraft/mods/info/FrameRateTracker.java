package media.jlt.minecraft.mods.info;

import java.util.Arrays;

final class FrameRateTracker {
	private static final long WINDOW_NANOS = 10_000_000_000L;
	private static final long RECALCULATE_INTERVAL_NANOS = 1_000_000_000L;
	private static final long MINIMUM_SAMPLE_NANOS = 2_000_000_000L;
	private static final long LOADING_GAP_NANOS = 1_000_000_000L;
	private static final int MINIMUM_SAMPLES = 60;
	private static final int CAPACITY = 16_384;

	private final long[] frameTimes = new long[CAPACITY];
	private final long[] sampleEndTimes = new long[CAPACITY];
	private final long[] sortingBuffer = new long[CAPACITY];

	private int nextIndex;
	private int size;
	private long previousFrameNanos;
	private long lastCalculationNanos;
	private int averageFps = -1;
	private int onePercentLow = -1;

	void recordFrame(long nowNanos) {
		if (previousFrameNanos == 0L) {
			previousFrameNanos = nowNanos;
			return;
		}

		long frameTime = nowNanos - previousFrameNanos;
		previousFrameNanos = nowNanos;
		if (frameTime <= 0L || frameTime > LOADING_GAP_NANOS) {
			resetSamples(nowNanos);
			return;
		}

		frameTimes[nextIndex] = frameTime;
		sampleEndTimes[nextIndex] = nowNanos;
		nextIndex = (nextIndex + 1) % CAPACITY;
		if (size < CAPACITY) {
			size++;
		}

		discardExpiredSamples(nowNanos - WINDOW_NANOS);
		if (nowNanos - lastCalculationNanos >= RECALCULATE_INTERVAL_NANOS) {
			calculateMetrics(nowNanos);
		}
	}

	int averageFps() {
		return averageFps;
	}

	int onePercentLow() {
		return onePercentLow;
	}

	void reset() {
		nextIndex = 0;
		size = 0;
		previousFrameNanos = 0L;
		lastCalculationNanos = 0L;
		averageFps = -1;
		onePercentLow = -1;
	}

	private void resetSamples(long nowNanos) {
		nextIndex = 0;
		size = 0;
		previousFrameNanos = nowNanos;
		lastCalculationNanos = nowNanos;
		averageFps = -1;
		onePercentLow = -1;
	}

	private void discardExpiredSamples(long cutoffNanos) {
		while (size > 0) {
			int oldestIndex = Math.floorMod(nextIndex - size, CAPACITY);
			if (sampleEndTimes[oldestIndex] >= cutoffNanos) {
				break;
			}
			size--;
		}
	}

	private void calculateMetrics(long nowNanos) {
		lastCalculationNanos = nowNanos;
		if (size < MINIMUM_SAMPLES) {
			averageFps = -1;
			onePercentLow = -1;
			return;
		}

		int oldestIndex = Math.floorMod(nextIndex - size, CAPACITY);
		if (nowNanos - sampleEndTimes[oldestIndex] < MINIMUM_SAMPLE_NANOS) {
			averageFps = -1;
			onePercentLow = -1;
			return;
		}

		long totalFrameTime = 0L;
		for (int i = 0; i < size; i++) {
			long frameTime = frameTimes[(oldestIndex + i) % CAPACITY];
			sortingBuffer[i] = frameTime;
			totalFrameTime += frameTime;
		}
		averageFps = totalFrameTime <= 0L
				? -1
				: Math.max(1, (int) Math.round(size * 1_000_000_000.0 / totalFrameTime));
		Arrays.sort(sortingBuffer, 0, size);
		int percentileIndex = Math.min(size - 1, Math.max(0, (int) Math.ceil(size * 0.99) - 1));
		long percentileFrameTime = sortingBuffer[percentileIndex];
		onePercentLow = percentileFrameTime <= 0L
				? -1
				: Math.max(1, (int) Math.round(1_000_000_000.0 / percentileFrameTime));
	}
}
