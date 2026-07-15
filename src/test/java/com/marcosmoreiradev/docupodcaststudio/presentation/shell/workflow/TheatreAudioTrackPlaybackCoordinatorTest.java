package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAudioTrackTimeline;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreAudioTrackTimelineEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreAudioTrackPlaybackCoordinatorTest {
    @TempDir Path temp;

    @Test
    void synchronizesOffsetPauseRateSeekAndStopIndependentlyFromNarration() throws Exception {
        Path audio = temp.resolve("ambiente.wav");
        Files.write(audio, new byte[]{1});
        FakePlayer player = new FakePlayer();
        TheatreAudioTrackPlaybackCoordinator coordinator = new TheatreAudioTrackPlaybackCoordinator(player);
        TheatreProjectLayer.TheatreAudioTrack track = new TheatreProjectLayer.TheatreAudioTrack(
                "TRACK-1", "AUDIO-1", "INTERVENCION-1", "SEG-1", 1.0, 6.0,
                TheatreProjectLayer.AudioTrackEndMode.SOURCE_TIME, 0.30, 8.0, true);
        TheatreAudioTrackTimeline timeline = new TheatreAudioTrackTimeline(List.of(
                new TheatreAudioTrackTimelineEntry(track, "ambiente.wav", "Ambiente", 2.0, 7.0,
                        List.of("SEG-1", "SEG-2"), List.of("INTERVENCION-1", "INTERVENCION-2"), true, "")));

        coordinator.sync(temp, timeline, 3.0, 1.0);
        assertTrue(player.playing);
        assertEquals(2.0, player.position, 0.001);
        assertEquals(0.30, player.volume, 0.001);
        assertEquals(6.0, player.stopAt, 0.001);
        assertEquals(0.5, player.fadeDuration, 0.001);

        coordinator.pause();
        assertTrue(player.paused);
        coordinator.resume();
        assertFalse(player.paused);
        coordinator.setPlaybackRate(1.5);
        assertEquals(1.5, player.rate, 0.001);

        player.position = 2.0;
        coordinator.sync(temp, timeline, 5.0, 1.5);
        assertEquals(4.0, player.position, 0.001, "Un salto debe reposicionar la pista dentro del archivo fuente.");

        coordinator.sync(temp, timeline, 8.0, 1.0);
        assertFalse(player.playing);
    }

    private static final class FakePlayer implements SegmentAudioPlayer {
        private boolean playing;
        private boolean paused;
        private double position;
        private double rate = 1.0;
        private double volume = 1.0;
        private double stopAt = Double.POSITIVE_INFINITY;
        private double fadeDuration;
        @Override public void play(Path audioFile, double startSeconds) { playing = true; paused = false; position = startSeconds; }
        @Override public void pause() { paused = true; }
        @Override public void resume() { paused = false; }
        @Override public void stop() { playing = false; paused = false; position = 0; }
        @Override public void setPlaybackRate(double rate) { this.rate = rate; }
        @Override public void setVolume(double volume) { this.volume = volume; }
        @Override public void setStopAtSeconds(double sourcePositionSeconds) { stopAt = sourcePositionSeconds; }
        @Override public void setFadeEnvelope(double sourceStartSeconds, double sourceEndSeconds, double fadeDurationSeconds) { fadeDuration = fadeDurationSeconds; }
        @Override public double playbackRate() { return rate; }
        @Override public boolean available() { return true; }
        @Override public boolean playing() { return playing; }
        @Override public double currentPositionSeconds() { return position; }
        @Override public String statusLabel() { return "fake"; }
    }
}
