package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE4: cue queue must wait for the real player before advancing. */
final class PlaybackCore4PlayerCompletionQueueSourceTest {
    @Test
    void shellWiresRealPlayerCompletionCallbackIntoSequentialQueue() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(shell.contains("setOnPlaybackFinished(this::handlePlaybackFinishedOnFxThread)"));
        assertTrue(shell.contains("private void handlePlaybackFinishedOnFxThread(Path audioFile)"));
        assertTrue(shell.contains("playbackTransport.sequentialActiveCue().or(() -> cueForCursor(manifest, cursor))"));
        assertTrue(shell.contains("playbackTransport.advanceSequentialAfterCurrentCueFinished(cue.get().unitId())"));
        assertTrue(transport.contains("sequentialQueue.advanceAfterCurrentCueFinished(unitId)"));
    }

    @Test
    void sequentialQueueWatchdogDoesNotCutAudioWhilePlayerIsStillSounding() throws IOException {
        String driver = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackSequentialQueueDriver.java"));
        assertTrue(driver.contains("BooleanSupplier playerPlaying"));
        assertTrue(driver.contains("if (!fromPlayerCallback && safePlayerPlaying())"));
        assertTrue(driver.contains("rescheduleShortProbe(id)"));
        assertTrue(driver.contains("advanceAfterCurrentCueFinished"));
        assertTrue(driver.contains("acceptedCompletionUnitId"));
        assertTrue(driver.contains("expectedUnitId.equals(currentUnitId)"));
    }
}
