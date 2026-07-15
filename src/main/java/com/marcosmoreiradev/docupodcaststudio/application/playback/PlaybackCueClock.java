package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;

/** Wall-clock guard that prevents cue advancement before the active WAV duration has elapsed. */
public final class PlaybackCueClock {
    private String activeUnitId = "";
    private double durationSeconds;
    private double offsetSeconds;
    private double pausedPositionSeconds;
    private double playbackRate = 1.0;
    private long startedNanos;
    private boolean active;
    private boolean paused;

    public void start(PlaybackCue cue, double startOffsetSeconds) {
        start(cue, startOffsetSeconds, playbackRate);
    }

    public void start(PlaybackCue cue, double startOffsetSeconds, double rate) {
        if (cue == null) {
            stop();
            return;
        }
        activeUnitId = cue.unitId();
        durationSeconds = Math.max(0.0, cue.durationSeconds());
        offsetSeconds = Math.max(0.0, Math.min(durationSeconds, startOffsetSeconds));
        pausedPositionSeconds = offsetSeconds;
        playbackRate = normalizeRate(rate);
        startedNanos = System.nanoTime();
        active = true;
        paused = false;
    }

    public void setPlaybackRate(double rate) {
        double normalized = normalizeRate(rate);
        if (active && !paused) {
            offsetSeconds = elapsedSeconds();
            startedNanos = System.nanoTime();
        } else if (active) {
            pausedPositionSeconds = elapsedSeconds();
            offsetSeconds = pausedPositionSeconds;
        }
        playbackRate = normalized;
    }

    public void pause() {
        if (active && !paused) {
            pausedPositionSeconds = elapsedSeconds();
            paused = true;
        }
    }

    public void resume() {
        if (active && paused) {
            offsetSeconds = pausedPositionSeconds;
            startedNanos = System.nanoTime();
            paused = false;
        }
    }

    public void stop() {
        activeUnitId = "";
        durationSeconds = 0.0;
        offsetSeconds = 0.0;
        pausedPositionSeconds = 0.0;
        playbackRate = 1.0;
        startedNanos = 0L;
        active = false;
        paused = false;
    }

    public boolean active() {
        return active && !paused && elapsedSeconds() + 0.05 < durationSeconds;
    }

    public boolean blockingCurrentCue(PlaybackCue cue) {
        return matches(cue) && active();
    }

    public double absolutePosition(PlaybackCue cue, double playerLocalPositionSeconds, double fallbackAbsolutePositionSeconds) {
        if (cue == null) {
            return Math.max(0.0, fallbackAbsolutePositionSeconds);
        }
        double local = playerLocalPositionSeconds >= 0.0 ? playerLocalPositionSeconds : Math.max(0.0, fallbackAbsolutePositionSeconds - cue.startSeconds());
        if (matches(cue)) {
            local = Math.max(local, elapsedSeconds());
        }
        return cue.startSeconds() + Math.min(cue.durationSeconds(), Math.max(0.0, local));
    }

    public boolean completed(PlaybackCue cue, double absolutePositionSeconds, double graceSeconds) {
        if (matches(cue)) {
            return elapsedSeconds() + Math.max(0.0, graceSeconds) >= durationSeconds;
        }
        return cue != null && absolutePositionSeconds + Math.max(0.0, graceSeconds) >= cue.endSeconds();
    }

    private double elapsedSeconds() {
        if (!active) {
            return 0.0;
        }
        if (paused) {
            return pausedPositionSeconds;
        }
        double elapsed = offsetSeconds + Math.max(0.0, (System.nanoTime() - startedNanos) / 1_000_000_000.0) * playbackRate;
        return Math.max(0.0, Math.min(durationSeconds, elapsed));
    }

    private boolean matches(PlaybackCue cue) {
        return active && cue != null && cue.unitId().equals(activeUnitId);
    }

    private static double normalizeRate(double rate) {
        if (rate >= 1.74) {
            return 1.75;
        }
        if (rate >= 1.49) {
            return 1.5;
        }
        return 1.0;
    }
}
