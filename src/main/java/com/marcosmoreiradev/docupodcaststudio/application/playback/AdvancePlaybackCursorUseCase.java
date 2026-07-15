package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

/** Advances a playback cursor against a playback manifest without depending on JavaFX or an audio device. */
public final class AdvancePlaybackCursorUseCase {
    public PlaybackCursor advance(PlaybackManifest manifest, PlaybackCursor cursor, double deltaSeconds) {
        if (cursor == null || cursor.paused() || manifest == null || manifest.emptyManifest()) {
            return cursor == null ? PlaybackCursor.stopped() : cursor;
        }
        double nextPosition = cursor.positionSeconds() + Math.max(0.0, deltaSeconds);
        if (nextPosition > manifest.totalDurationSeconds()) {
            return PlaybackCursor.stopped();
        }
        return manifest.cueAt(nextPosition)
                .map(cue -> new PlaybackCursor(cue.segmentId(), nextPosition, false))
                .orElseGet(() -> manifest.cues().stream()
                        .filter(cue -> cue.startSeconds() >= nextPosition)
                        .findFirst()
                        .map(cue -> new PlaybackCursor(cue.segmentId(), cue.startSeconds(), false))
                        .orElse(PlaybackCursor.stopped()));
    }

    public PlaybackCursor cursorAt(PlaybackManifest manifest, double absolutePositionSeconds, boolean paused) {
        if (manifest == null || manifest.emptyManifest()) {
            return PlaybackCursor.stopped();
        }
        double position = Math.max(0.0, Math.min(absolutePositionSeconds, manifest.totalDurationSeconds()));
        return manifest.cueAt(position)
                .map(cue -> new PlaybackCursor(cue.segmentId(), position, paused))
                .orElseGet(() -> manifest.firstCue()
                        .map(cue -> new PlaybackCursor(cue.segmentId(), cue.startSeconds(), paused))
                        .orElse(PlaybackCursor.stopped()));
    }
}
