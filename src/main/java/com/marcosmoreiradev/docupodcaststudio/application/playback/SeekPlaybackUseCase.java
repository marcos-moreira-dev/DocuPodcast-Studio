package com.marcosmoreiradev.docupodcaststudio.application.playback;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;

/** Pure use case for seeking playback to a clicked segment/line or render unit. */
public final class SeekPlaybackUseCase {
    public PlaybackCursor seek(PlaybackCursor current, String segmentId, double positionSeconds) {
        PlaybackCursor base = current == null ? PlaybackCursor.stopped() : current;
        return base.seekTo(segmentId, positionSeconds);
    }

    public PlaybackCursor seek(PlaybackCursor current, PlaybackManifest manifest, String segmentId) {
        PlaybackCursor base = current == null ? PlaybackCursor.stopped() : current;
        if (manifest == null) {
            return base.seekTo(segmentId, 0.0);
        }
        return manifest.cueForSegment(segmentId)
                .map(cue -> base.seekTo(cue.segmentId(), cue.startSeconds()))
                .orElseGet(() -> base.seekTo(segmentId, 0.0));
    }

    /**
     * TI7 unit-level seek. The cursor remains segment-compatible, but the
     * absolute position targets the selected render/oration unit.
     */
    public PlaybackCursor seekUnit(PlaybackCursor current, PlaybackManifest manifest, String unitId) {
        PlaybackCursor base = current == null ? PlaybackCursor.stopped() : current;
        if (manifest == null) {
            return base;
        }
        return manifest.cueForUnit(unitId)
                .map(cue -> base.seekTo(cue.segmentId(), cue.startSeconds()))
                .orElse(base);
    }
}
