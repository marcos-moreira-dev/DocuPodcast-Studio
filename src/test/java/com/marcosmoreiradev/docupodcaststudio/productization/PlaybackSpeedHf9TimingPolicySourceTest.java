package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-SPEED-HF9: accelerated chunks must advance by real completion, not by a stale 1x wait. */
final class PlaybackSpeedHf9TimingPolicySourceTest {
    @Test
    void playbackTimingConstantsAreNamedAndShared() throws Exception {
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTimingPolicy.java");
        String queue = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackSequentialQueueDriver.java");
        String deadline = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackCueDeadlineSequencer.java");

        assertTrue(policy.contains("watchdogSafetyMarginSeconds"));
        assertTrue(policy.contains("deadlineSafetyMarginSeconds"));
        assertTrue(policy.contains("minimumWatchdogDelayMillis"));
        assertTrue(policy.contains("completionGraceSeconds"));
        assertTrue(queue.contains("timing.watchdogDelayMillis"));
        assertTrue(deadline.contains("timing.deadlineDelayMillis"));
        assertFalse(queue.contains("+ 1.25"));
        assertFalse(deadline.contains("+ 0.75"));
    }

    @Test
    void shellAcceptsNaturalCompletionPathRobustlyBeforeFallingBackToWatchdog() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String transport = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java");

        assertTrue(shell.contains("playbackTransport.matchesCompletedAudioFile(cue.get(), currentProjectFile().orElse(null), audioFile)"));
        assertTrue(transport.contains("matchesCompletedAudioFile(PlaybackCue cue, Path projectFile, Path completedFile)"));
        assertTrue(transport.contains("if (expected.equals(completed))"));
        assertTrue(transport.contains("expectedName.equals(completedName)"));
        assertTrue(shell.contains("playbackTransport.advanceSequentialAfterCurrentCueFinished(cue.get().unitId())"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
