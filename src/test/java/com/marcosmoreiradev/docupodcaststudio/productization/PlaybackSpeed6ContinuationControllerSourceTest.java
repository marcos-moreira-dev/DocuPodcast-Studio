package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-SPEED6: playback continuation is a controller plus real Java Sound completion event. */
final class PlaybackSpeed6ContinuationControllerSourceTest {
    @Test
    void segmentPlayerReportsNaturalEndWithCompletedAudioPath() throws IOException {
        String port = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/playback/SegmentAudioPlayer.java");
        String player = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/playback/JavaSoundSegmentAudioPlayer.java");
        assertTrue(port.contains("setOnPlaybackFinished(Consumer<Path> callback)"));
        assertTrue(player.contains("Consumer<Path> onPlaybackFinished"));
        assertTrue(player.contains("reachedNaturalEnd"));
        assertTrue(player.contains("onPlaybackFinished.accept(completedFile)"));
    }

    @Test
    void shellUsesContinuationControllerAndIgnoresStaleCompletionEvents() throws IOException {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String transport = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java");
        assertTrue(transport.contains("private final PlaybackContinuationController continuation"));
        assertTrue(shell.contains("advanceAfterPlayerFinished(Path audioFile)"));
        assertTrue(shell.contains("playbackTransport.matchesCompletedAudioFile(cue.get(), currentProjectFile().orElse(null), audioFile)"));
        assertTrue(shell.contains("advanceAfterCompletedCue(PlaybackCue completedCue)"));
    }

    @Test
    void controllerOwnsCueClockGuardAndNextCueResolution() throws IOException {
        String controller = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackContinuationController.java");
        assertTrue(controller.contains("private final PlaybackCueClock clock"));
        assertTrue(controller.contains("activeUnitId"));
        assertTrue(controller.contains("playerFinishedUnitId"));
        assertTrue(controller.contains("nextCue(PlaybackManifest manifest, PlaybackCue completedCue)"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
