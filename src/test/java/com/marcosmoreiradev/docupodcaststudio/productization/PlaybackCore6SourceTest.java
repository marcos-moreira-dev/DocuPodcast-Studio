package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE6 keeps sequential playback continuation owned by the queue, not by timer ticks. */
final class PlaybackCore6SourceTest {
    @Test
    void sequentialQueueIsSingleContinuationAuthority() throws Exception {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String queue = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackSequentialQueueDriver.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));

        assertTrue(viewModel.contains("if (playbackTransport.sequentialActive()) { return; }"));
        assertTrue(transport.contains("if (!sequentialQueue.active())"));
        assertTrue(viewModel.contains("playbackTransport.advanceSequentialAfterCurrentCueFinished(cue.get().unitId())"));
        assertTrue(transport.contains("sequentialQueue.advanceAfterCurrentCueFinished()"));
        assertTrue(queue.contains("CueStarter"));
        assertTrue(queue.contains("advanceIfCurrent(long id, boolean fromPlayerCallback, String expectedUnitId)"));
        assertTrue(queue.contains("safePlayerPlaying()"));
    }
}
