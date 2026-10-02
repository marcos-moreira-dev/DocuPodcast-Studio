package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;

/** Decides whether a priority gap-fill job may leave compatible playback running. */
public final class PriorityAudioPlaybackPolicy {
    public boolean keepPlayback(boolean preserveInterruptedJob, PlaybackCursor cursor) {
        return preserveInterruptedJob && cursor != null && !cursor.stoppedState();
    }
}
