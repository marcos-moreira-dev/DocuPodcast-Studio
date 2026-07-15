package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAudioTrackTimeline;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAudioTrackTimelineEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps the exclusive theatre background track synchronized with narration transport. */
public final class TheatreAudioTrackPlaybackCoordinator {
    private final SegmentAudioPlayer player;
    private String activeTrackId = "";
    private double lastAbsolutePosition;

    public TheatreAudioTrackPlaybackCoordinator(SegmentAudioPlayer player) {
        this.player = player;
    }

    public void sync(Path projectDirectory,
                     TheatreAudioTrackTimeline timeline,
                     double absolutePositionSeconds,
                     double playbackRate) {
        lastAbsolutePosition = Math.max(0.0, absolutePositionSeconds);
        TheatreAudioTrackTimelineEntry entry = timeline == null ? null
                : timeline.activeAt(lastAbsolutePosition).orElse(null);
        if (entry == null || projectDirectory == null || !entry.valid()) {
            stop();
            return;
        }
        double expectedSourceOffset = entry.track().sourceStartSeconds()
                + Math.max(0.0, lastAbsolutePosition - entry.timelineStartSeconds());
        if (entry.track().id().equals(activeTrackId) && player.playing()
                && Math.abs(player.currentPositionSeconds() - expectedSourceOffset) <= 0.35) {
            player.setPlaybackRate(playbackRate);
            return;
        }
        Path audio = projectDirectory.resolve(entry.assetRelativePath()).normalize();
        if (!Files.isRegularFile(audio)) {
            stop();
            return;
        }
        double sourceOffset = expectedSourceOffset;
        if (sourceOffset >= entry.track().effectiveEndSeconds()) {
            stop();
            return;
        }
        try {
            player.setPlaybackRate(playbackRate);
            player.setVolume(entry.track().volume());
            player.setStopAtSeconds(entry.track().effectiveEndSeconds());
            player.setFadeEnvelope(entry.track().sourceStartSeconds(), entry.track().effectiveEndSeconds(),
                    entry.track().fadeDurationSeconds());
            player.play(audio, sourceOffset);
            activeTrackId = entry.track().id();
        } catch (IOException | RuntimeException ex) {
            stop();
        }
    }

    public void pause() {
        if (player.playing()) player.pause();
    }

    public void resume() {
        if (!activeTrackId.isBlank()) player.resume();
    }

    public void setPlaybackRate(double rate) {
        player.setPlaybackRate(rate);
    }

    public void stop() {
        player.stop();
        activeTrackId = "";
    }

    public String activeTrackId() {
        return activeTrackId;
    }
}
