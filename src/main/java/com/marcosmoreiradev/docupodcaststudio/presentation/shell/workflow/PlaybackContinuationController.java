package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.playback.PlaybackCueClock;
import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Owns the runtime state needed to continue document playback from one WAV cue to the next.
 *
 * <p>The shell still decides user-facing messages and buffer policy, but this controller is the
 * single source of truth for: active cue id, wall-clock cue timing, short restart guards and the
 * completion event emitted by the Java Sound player. This avoids scattering continuation rules
 * across playbar actions, speed buttons and timer ticks.</p>
 */
public final class PlaybackContinuationController {
    private final PlaybackTimingPolicy timing = PlaybackTimingPolicy.defaults();
    private final SegmentAudioPlayer player;
    private final PlaybackCueClock clock = new PlaybackCueClock();
    private String activeUnitId = "";
    private String playerFinishedUnitId = "";
    private long transitionGuardUntilNanos;

    public PlaybackContinuationController(SegmentAudioPlayer player) {
        this.player = player;
    }

    public void startCue(PlaybackCue cue, double startOffsetSeconds, double playbackRate, long guardMillis) {
        if (cue == null) {
            stop();
            return;
        }
        activeUnitId = cue.unitId();
        playerFinishedUnitId = "";
        armTransitionGuard(guardMillis);
        clock.start(cue, startOffsetSeconds, playbackRate);
    }

    public void pause() {
        clock.pause();
    }

    public void resume() {
        clock.resume();
    }

    public void setPlaybackRate(double rate) {
        clock.setPlaybackRate(rate);
    }

    public void stop() {
        clock.stop();
        activeUnitId = "";
        playerFinishedUnitId = "";
        transitionGuardUntilNanos = 0L;
    }

    public boolean active() {
        return clock.active();
    }

    public String activeUnitId() {
        return activeUnitId;
    }

    public void markPlayerFinished() {
        if (!activeUnitId.isBlank()) {
            playerFinishedUnitId = activeUnitId;
        }
    }

    public Optional<PlaybackCue> activeCue(PlaybackManifest manifest, PlaybackCursor cursor) {
        if (manifest == null || cursor == null || cursor.segmentId().isBlank()) {
            return Optional.empty();
        }
        if (!activeUnitId.isBlank()) {
            Optional<PlaybackCue> active = manifest.cueForUnit(activeUnitId)
                    .filter(cue -> cue.segmentId().equals(cursor.segmentId()));
            if (active.isPresent()) {
                return active;
            }
        }
        double position = cursor.positionSeconds();
        Optional<PlaybackCue> byPosition = manifest.cueAt(position)
                .filter(found -> found.segmentId().equals(cursor.segmentId()));
        if (byPosition.isPresent()) {
            return byPosition;
        }
        List<PlaybackCue> segmentCues = manifest.cuesForSegment(cursor.segmentId());
        Optional<PlaybackCue> nearestUnit = segmentCues.stream()
                .filter(cue -> position + 0.25 >= cue.startSeconds() && position <= cue.endSeconds() + 0.25)
                .min(Comparator.comparingDouble(cue -> Math.abs(cue.startSeconds() - position)));
        if (nearestUnit.isPresent()) {
            return nearestUnit;
        }
        return segmentCues.size() == 1 ? Optional.of(segmentCues.getFirst()) : Optional.empty();
    }

    public double absolutePosition(PlaybackCue cue, PlaybackCursor cursor) {
        double playerPosition = player != null && player.available() ? player.currentPositionSeconds() : -1.0;
        double fallback = cursor == null ? 0.0 : cursor.positionSeconds() + 0.25;
        return clock.absolutePosition(cue, playerPosition, fallback);
    }

    public boolean shouldIgnoreTransientStop() {
        return transitionGuardActive() && player != null && !player.playing();
    }

    public boolean shouldAdvance(PlaybackCue cue, double absolutePositionSeconds) {
        if (cue == null) {
            return false;
        }
        boolean playerFinishedThisCue = cue.unitId().equals(playerFinishedUnitId) && !transitionGuardActive();
        return playerFinishedThisCue
                || clock.completed(cue, absolutePositionSeconds, timing.completionGraceSeconds());
    }

    public boolean blockingCurrentCue(PlaybackCue cue) {
        return clock.blockingCurrentCue(cue);
    }

    public Optional<PlaybackCue> nextCue(PlaybackManifest manifest, PlaybackCue completedCue) {
        if (manifest == null || completedCue == null) {
            return Optional.empty();
        }
        return manifest.nextCueAfterUnit(completedCue.unitId());
    }

    public void consumePlayerFinishedMarker() {
        playerFinishedUnitId = "";
    }

    public void armTransitionGuard(long millis) {
        transitionGuardUntilNanos = System.nanoTime() + Math.max(0L, millis) * 1_000_000L;
    }

    public boolean transitionGuardActive() {
        return transitionGuardUntilNanos > System.nanoTime();
    }
}
