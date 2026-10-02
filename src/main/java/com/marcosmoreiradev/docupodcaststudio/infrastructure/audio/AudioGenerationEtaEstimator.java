package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/** Estimates remaining synthesis time from actual completed voice-unit intervals. */
final class AudioGenerationEtaEstimator {
    record Estimate(long remainingSeconds, long errorSeconds) {
        static final Estimate UNKNOWN = new Estimate(0L, 0L);
    }

    private final LongSupplier nanoTime;
    private final List<Double> samplesSeconds = new ArrayList<>();
    private long lastCompletionNanos;
    private boolean firstCompletionObserved;

    AudioGenerationEtaEstimator() {
        this(System::nanoTime);
    }

    AudioGenerationEtaEstimator(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
        this.lastCompletionNanos = nanoTime.getAsLong();
    }

    synchronized void recordCompletion() {
        long now = nanoTime.getAsLong();
        /*
         * The first completion includes engine startup, model loading and warm-up.
         * Use it only to establish the steady-state baseline. The interval between
         * the first and second completed units is the first representative sample.
         */
        if (!firstCompletionObserved) {
            firstCompletionObserved = true;
            lastCompletionNanos = now;
            return;
        }
        double seconds = Math.max(0.001, (now - lastCompletionNanos) / 1_000_000_000.0);
        lastCompletionNanos = now;
        samplesSeconds.add(seconds);
        if (samplesSeconds.size() > 120) samplesSeconds.removeFirst();
    }

    synchronized Estimate estimate(int remainingUnits) {
        if (remainingUnits <= 0 || samplesSeconds.isEmpty()) return Estimate.UNKNOWN;
        double mean = samplesSeconds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = samplesSeconds.stream()
                .mapToDouble(sample -> (sample - mean) * (sample - mean))
                .sum() / Math.max(1, samplesSeconds.size() - 1);
        double deviation = Math.sqrt(Math.max(0.0, variance));
        double remaining = mean * remainingUnits;
        // Combine uncertainty of the observed mean and the future-unit variance.
        double statisticalError = 1.96 * deviation
                * (Math.sqrt(remainingUnits) + remainingUnits / Math.sqrt(samplesSeconds.size()));
        // A single post-warm-up interval gives an early but deliberately broad ETA.
        double minimumError = Math.max(1.0, remaining
                * (samplesSeconds.size() == 1 ? 0.50 : 0.10));
        return new Estimate(Math.max(1L, Math.round(remaining)),
                Math.max(1L, Math.round(Math.max(minimumError, statisticalError))));
    }
}
