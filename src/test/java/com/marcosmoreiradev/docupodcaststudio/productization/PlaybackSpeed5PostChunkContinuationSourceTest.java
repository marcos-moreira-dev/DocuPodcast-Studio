package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE4: playback continuation is driven by real player completion and exact cue queue state. */
final class PlaybackSpeed5PostChunkContinuationSourceTest {
    @Test
    void shellDelegatesContinuationRulesToControllerAndPlayerCompletionCallback() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(transport.contains("PlaybackContinuationController"));
        assertTrue(shell.contains("setOnPlaybackFinished(this::handlePlaybackFinishedOnFxThread)"));
        assertTrue(shell.contains("Platform.runLater(() -> advanceAfterPlayerFinished(audioFile))"));
        assertTrue(shell.contains("advanceAfterPlayerFinished(Path audioFile)"));
        assertTrue(shell.contains("advanceAfterCompletedCue(PlaybackCue completedCue)"));
        assertTrue(shell.contains("playbackTransport.advanceSequentialAfterCurrentCueFinished(cue.get().unitId())"));
        assertTrue(transport.contains("sequentialQueue.advanceAfterCurrentCueFinished()"));
    }

    @Test
    void continuationControllerOwnsClockGuardAndPlayerFinishedMarker() throws IOException {
        String controller = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackContinuationController.java"));
        assertTrue(controller.contains("private final PlaybackCueClock clock"));
        assertTrue(controller.contains("playerFinishedUnitId"));
        assertTrue(controller.contains("shouldAdvance"));
        assertTrue(controller.contains("transitionGuardActive"));
    }
}
