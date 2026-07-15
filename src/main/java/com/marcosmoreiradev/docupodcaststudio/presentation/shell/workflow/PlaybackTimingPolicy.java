package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PunctuationPausePolicy;

/**
 * Central timing policy for document playback continuation.
 *
 * <p>Playback speed changes must affect both the audio stream and the moment when the next chunk is
 * allowed to start. The real player completion callback remains the preferred signal; watchdogs are
 * only short safety nets for missed callbacks, not a second 1x-duration wait.</p>
 */
public final class PlaybackTimingPolicy {
    private static final PlaybackTimingPolicy DEFAULTS = new PlaybackTimingPolicy(
            0.20,
            0.20,
            250L,
            150L,
            150L,
            0.05,
            350L,
            1000L);

    private final double watchdogSafetyMarginSeconds;
    private final double deadlineSafetyMarginSeconds;
    private final long minimumWatchdogDelayMillis;
    private final long shortProbeDelayMillis;
    private final long pollIntervalMillis;
    private final double completionGraceSeconds;
    private final long transitionGuardMillis;
    private final long interCuePauseMillis;
    private final PunctuationPausePolicy punctuationPausePolicy = new PunctuationPausePolicy();

    private PlaybackTimingPolicy(double watchdogSafetyMarginSeconds,
                                 double deadlineSafetyMarginSeconds,
                                 long minimumWatchdogDelayMillis,
                                 long shortProbeDelayMillis,
                                 long pollIntervalMillis,
                                 double completionGraceSeconds,
                                 long transitionGuardMillis,
                                 long interCuePauseMillis) {
        this.watchdogSafetyMarginSeconds = Math.max(0.0, watchdogSafetyMarginSeconds);
        this.deadlineSafetyMarginSeconds = Math.max(0.0, deadlineSafetyMarginSeconds);
        this.minimumWatchdogDelayMillis = Math.max(1L, minimumWatchdogDelayMillis);
        this.shortProbeDelayMillis = Math.max(1L, shortProbeDelayMillis);
        this.pollIntervalMillis = Math.max(1L, pollIntervalMillis);
        this.completionGraceSeconds = Math.max(0.0, completionGraceSeconds);
        this.transitionGuardMillis = Math.max(0L, transitionGuardMillis);
        this.interCuePauseMillis = Math.max(0L, interCuePauseMillis);
    }

    public static PlaybackTimingPolicy defaults() {
        return DEFAULTS;
    }

    public long watchdogDelayMillis(double cueDurationSeconds, double localOffsetSeconds, double playbackRate) {
        return delayMillis(cueDurationSeconds, localOffsetSeconds, playbackRate,
                watchdogSafetyMarginSeconds, minimumWatchdogDelayMillis);
    }

    public long deadlineDelayMillis(double cueDurationSeconds, double localOffsetSeconds, double playbackRate) {
        return delayMillis(cueDurationSeconds, localOffsetSeconds, playbackRate,
                deadlineSafetyMarginSeconds, minimumWatchdogDelayMillis);
    }

    public long shortProbeDelayMillis() {
        return shortProbeDelayMillis;
    }

    public long pollIntervalMillis() {
        return pollIntervalMillis;
    }

    public double completionGraceSeconds() {
        return completionGraceSeconds;
    }

    public long transitionGuardMillis() {
        return transitionGuardMillis;
    }

    public long interCuePauseMillis() {
        return interCuePauseMillis;
    }

    public long interCuePauseMillis(PlaybackCue completedCue, PlaybackCue nextCue, double playbackRate) {
        return punctuationPausePolicy.interCuePauseMillis(
                completedCue,
                nextCue,
                normalizeRate(playbackRate),
                interCuePauseMillis);
    }

    public double normalizeRate(double rate) {
        if (rate >= 1.74) {
            return 1.75;
        }
        if (rate >= 1.49) {
            return 1.5;
        }
        return 1.0;
    }

    private long delayMillis(double cueDurationSeconds, double localOffsetSeconds, double playbackRate,
                             double safetyMarginSeconds, long minimumDelayMillis) {
        double remaining = Math.max(0.1, Math.max(0.0, cueDurationSeconds) - Math.max(0.0, localOffsetSeconds));
        double normalizedRate = normalizeRate(playbackRate);
        return Math.max(minimumDelayMillis, Math.round((remaining / normalizedRate + safetyMarginSeconds) * 1000.0));
    }
}
