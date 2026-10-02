package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioGenerationEtaEstimatorTest {
    @Test
    void excludesModelWarmupAndEstimatesFromPostWarmupIntervals() {
        AtomicLong clock = new AtomicLong();
        AudioGenerationEtaEstimator estimator =
                new AudioGenerationEtaEstimator(clock::get);

        clock.addAndGet(10_000_000_000L);
        estimator.recordCompletion();
        clock.addAndGet(14_000_000_000L);
        estimator.recordCompletion();
        clock.addAndGet(12_000_000_000L);
        estimator.recordCompletion();

        AudioGenerationEtaEstimator.Estimate estimate = estimator.estimate(10);

        assertEquals(130L, estimate.remainingSeconds());
        assertTrue(estimate.errorSeconds() >= 13L);
    }

    @Test
    void waitsUntilTheSecondCompletionBeforeClaimingAnEta() {
        AtomicLong clock = new AtomicLong();
        AudioGenerationEtaEstimator estimator =
                new AudioGenerationEtaEstimator(clock::get);
        clock.addAndGet(5_000_000_000L);
        estimator.recordCompletion();

        assertEquals(AudioGenerationEtaEstimator.Estimate.UNKNOWN,
                estimator.estimate(20));
    }

    @Test
    void publishesAnEarlyBroadEstimateFromTheSecondCompletedUnit() {
        AtomicLong clock = new AtomicLong();
        AudioGenerationEtaEstimator estimator =
                new AudioGenerationEtaEstimator(clock::get);

        clock.addAndGet(60_000_000_000L); // carga del modelo: no es una muestra
        estimator.recordCompletion();
        clock.addAndGet(15_000_000_000L); // primer intervalo en régimen
        estimator.recordCompletion();

        AudioGenerationEtaEstimator.Estimate estimate = estimator.estimate(10);

        assertEquals(150L, estimate.remainingSeconds());
        assertEquals(75L, estimate.errorSeconds());
    }
}
