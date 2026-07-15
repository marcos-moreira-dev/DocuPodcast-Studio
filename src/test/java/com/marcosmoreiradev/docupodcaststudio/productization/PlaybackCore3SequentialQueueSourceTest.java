package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE4: sequential queue uses real player completion plus conservative watchdog. */
final class PlaybackCore3SequentialQueueSourceTest {
    @Test
    void sequentialQueueDriverOwnsCueOrderAndUsesPlayerCompletionBeforeWatchdogAdvance() throws IOException {
        String driver = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackSequentialQueueDriver.java"));
        assertTrue(driver.contains("class PlaybackSequentialQueueDriver"));
        assertTrue(driver.contains("ScheduledExecutorService"));
        assertTrue(driver.contains("startCurrentCue"));
        assertTrue(driver.contains("advanceIfCurrent"));
        assertTrue(driver.contains("advanceAfterCurrentCueFinished"));
        assertTrue(driver.contains("safePlayerPlaying"));
        assertTrue(driver.contains("rescheduleShortProbe"));
        assertTrue(driver.contains("Platform.runLater"));
    }

    @Test
    void shellDoesNotLetTimerAdvanceWhenSequentialQueueIsActive() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(shell.contains("if (playbackTransport.sequentialActive())"));
        assertTrue(transport.contains("sequentialQueue.start(manifest, cue, playbackRate"));
        assertTrue(shell.contains("playExactCue(cue, 0.0)"));
        assertTrue(shell.contains("restartActiveCueImmediatelyAtRate"));
        assertTrue(shell.contains("Continuando lectura secuencial exacta"));
        assertTrue(shell.contains("playbackTransport.setSequentialPlaybackRate(normalizedRate, 0.0)"));
    }
}
