package com.marcosmoreiradev.docupodcaststudio.domain.playback;

/** Current playback cursor. It supports pause/resume and seek from a clicked line/segment. */
public record PlaybackCursor(String segmentId, double positionSeconds, boolean paused) {
    public PlaybackCursor {
        segmentId = segmentId == null ? "" : segmentId.strip();
        positionSeconds = Math.max(0.0, positionSeconds);
    }

    public static PlaybackCursor stopped() {
        return new PlaybackCursor("", 0.0, true);
    }

    public boolean stoppedState() {
        return segmentId.isBlank() && paused && positionSeconds == 0.0;
    }

    public boolean playing() {
        return !paused && !segmentId.isBlank();
    }

    public PlaybackCursor pause() {
        return new PlaybackCursor(segmentId, positionSeconds, true);
    }

    public PlaybackCursor resume() {
        return new PlaybackCursor(segmentId, positionSeconds, false);
    }

    public PlaybackCursor at(double absolutePositionSeconds) {
        return new PlaybackCursor(segmentId, absolutePositionSeconds, paused);
    }

    public PlaybackCursor seekTo(String targetSegmentId, double targetPositionSeconds) {
        return new PlaybackCursor(targetSegmentId, targetPositionSeconds, paused);
    }
}
