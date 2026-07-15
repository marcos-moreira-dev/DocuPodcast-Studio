package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE2: continuation is driven by cue deadlines, not only by player callbacks/timer ticks. */
final class PlaybackCore2DeadlineSequencerSourceTest {
    @Test
    void shellDelegatesCueCompletionDeadlineToDedicatedSequencer() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(transport.contains("PlaybackCueDeadlineSequencer"));
        assertTrue(transport.contains("deadlineSequencer.start"));
        assertTrue(shell.contains("this::advanceAfterCompletedCue"));
        assertTrue(shell.contains("playbackTransport.stopCueMonitoring()"));
        assertTrue(transport.contains("deadlineSequencer.stop()"));
    }

    @Test
    void sequencerUsesWallClockDeadlineAsAThirdCompletionSignal() throws IOException {
        String sequencer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackCueDeadlineSequencer.java"));
        assertTrue(sequencer.contains("scheduleAtFixedRate"));
        assertTrue(sequencer.contains("timing.pollIntervalMillis()"));
        assertTrue(sequencer.contains("deadlineTask = executor.schedule"));
        assertTrue(sequencer.contains("PlaybackTimingPolicy timing"));
        assertTrue(sequencer.contains("timing.deadlineDelayMillis"));
        assertTrue(sequencer.contains("Platform.runLater(() ->"));
        assertTrue(sequencer.contains("completed.accept(cue)"));
    }
}
