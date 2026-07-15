package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-CORE3: playback now uses a sequential queue driver instead of resolving from selection after each chunk. */
final class PlaybackSpeed8WatchdogContinuationSourceTest {
    @Test
    void shellDelegatesContinuousPlaybackToSequentialQueueDriver() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(transport.contains("PlaybackRuntimeQueue runtimeQueue"));
        assertTrue(transport.contains("PlaybackSequentialQueueDriver sequentialQueue"));
        assertTrue(shell.contains("startSequentialPlayback(manifest, requestedCue.get(), bufferedStart)"));
        assertTrue(shell.contains("startCueFromSequentialQueue"));
        assertTrue(shell.contains("restartActiveCueImmediatelyAtRate(activeCue.get(), normalized)"));
    }

    @Test
    void bufferContinuationStartsTheExactNextCueInsteadOfReResolvingOldSelection() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(shell.contains("if (transitionToCue(nextCue.get()))"));
        assertTrue(shell.contains("Buffer recuperado. Continuando después"));
    }

    @Test
    void runtimeQueueIsUsedBeforeFallingBackToManifestResolution() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));
        assertTrue(shell.contains("playbackTransport.refreshRuntimeQueue(manifest)"));
        assertTrue(shell.contains("Optional<PlaybackCue> next = playbackTransport.nextRuntimeCueAfter(completedCue)"));
        assertTrue(transport.contains("runtimeQueue.diagnosticLabel()"));
    }

    @Test
    void selectedCueQueueStartDoesNotCaptureReassignedManifestInLambda() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(shell.contains("PlaybackManifest fragmentManifest = manifest.onlySegment(segment.get().id())"));
        assertTrue(shell.contains("PlaybackCue selectedCue = fragmentManifest.firstCue().orElseThrow()"));
        assertTrue(shell.contains("playbackTransport.startRuntimeQueue(fragmentManifest, selectedCue)"));
        String forbiddenCapture = "ifPresent(cue -> " + "playbackRuntimeQueue.start(manifest, cue))";
        assertFalse(shell.contains(forbiddenCapture));
    }
}
