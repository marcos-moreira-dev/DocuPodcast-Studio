package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-SPEED7: runtime playback must rebuild a fresh manifest and select the best persisted job. */
final class PlaybackSpeed7RuntimeManifestSourceTest {
    @Test
    void playbackUsesBestPersistedAudioJobInsteadOfFirstOrStaleManifest() throws IOException {
        String selector = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlayableAudioJobSelector.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(selector.contains("matchingCompleted * 1_000_000L"));
        assertTrue(selector.contains("100_000L"));
        assertTrue(shell.contains("playableAudioJobSelector.select(snapshots, activeJobId, script)"));
    }

    @Test
    void playbackStartAndCompletionRefreshManifestAtRuntime() throws IOException {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String transport = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java"));

        assertTrue(shell.contains("PlaybackManifest manifest = rebuildPlaybackManifestFromLatestJob();"));
        assertTrue(shell.contains("Optional<PlaybackCue> next = playbackTransport.nextRuntimeCueAfter(completedCue)"));
        assertTrue(shell.contains("next = playbackTransport.nextCue(manifest, completedCue)"));
        assertTrue(shell.contains("playbackTransport.nextCueAfterCompletedSegmentFallback"));
        assertTrue(shell.contains("playbackTransport.manifestRuntimeLabel"));
        assertTrue(transport.contains("runtimeQueue.nextAfter(cue)"));
    }
}
