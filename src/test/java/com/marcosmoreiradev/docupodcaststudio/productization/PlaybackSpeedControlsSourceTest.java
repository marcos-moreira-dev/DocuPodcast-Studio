package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaybackSpeedControlsSourceTest {
    @Test
    void segmentPlayerExposesRateAndJavaSoundRestartsCurrentChunk() throws IOException {
        String port = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/SegmentAudioPlayer.java");
        String player = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/JavaSoundSegmentAudioPlayer.java");
        assertTrue(port.contains("setPlaybackRate(double rate)"));
        assertTrue(port.contains("double playbackRate()"));
        assertTrue(player.contains("PcmTimeStretchProcessor.speedUpPreservePitch"));
        assertTrue(player.contains("speedModeLabel(rate)"));
        assertTrue(player.contains("currentPositionSeconds()"));
        assertTrue(player.contains("play(file, position)"));
        assertTrue(player.contains("Velocidad de lectura cambiada"));
        assertTrue(player.contains("tono natural"));
    }

    @Test
    void cueClockAndShellKeepPlaybackRateForBufferedContinuation() throws IOException {
        String clock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/PlaybackCueClock.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String transport = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java");
        assertTrue(clock.contains("setPlaybackRate(double rate)"));
        assertTrue(clock.contains("* playbackRate"));
        assertTrue(shell.contains("private final DoubleProperty playbackRate"));
        assertTrue(shell.contains("setPlaybackRate(double rate)"));
        assertTrue(shell.contains("playbackTransport.setPlaybackRate(normalized)"));
        assertTrue(transport.contains("player.setPlaybackRate(rate)"));
        assertTrue(transport.contains("continuation.setPlaybackRate(rate)"));
        assertTrue(transport.contains("sequentialQueue.setPlaybackRate(rate, 0.0)"));
        assertTrue(shell.contains("restartActiveCueImmediatelyAtRate(activeCue.get(), normalized)"));
        assertTrue(shell.contains("private final PlaybackTransportCoordinator playbackTransport"));
        assertTrue(transport.contains("continuation.activeCue(manifest, cursor)"));
        assertTrue(shell.contains("cueForCursor(PlaybackManifest manifest, PlaybackCursor cursor)"));
        assertTrue(shell.contains("cursor == null || cursor.stoppedState()"));
        assertTrue(shell.contains("status.completedSegments() > 0"));
        assertTrue(shell.lines().count() <= 2600, "La velocidad no debe superar el limite RF2.");
    }

    @Test
    void floatingPlaybarOffersHumanSpeedButtons() throws IOException {
        String controls = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String css = read("src/main/resources/css/components/actions.css");
        assertTrue(controls.contains("1x"));
        assertTrue(controls.contains("1.5x"));
        assertTrue(controls.contains("1.75x"));
        assertTrue(controls.contains("ui-playback-speed-controls"));
        assertTrue(controls.contains("ui-playback-speed-active"));
        assertTrue(workspace.contains("viewModel.playbackRateProperty()"));
        assertTrue(workspace.contains("viewModel.setPlaybackRate(1.5)"));
        assertTrue(workspace.contains("viewModel.setPlaybackRate(1.75)"));
        assertTrue(css.contains("PLAYBACK-SPEED1"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
