package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAYBACK-SPEED4: changing speed must not duplicate a cue or break continuation. */
final class PlaybackSpeed3ContinuitySourceTest {
    @Test
    void rateChangeResynchronizesActiveCueAndArmsShortTransitionGuard() throws IOException {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertTrue(shell.contains("Optional<PlaybackCue> activeCue = cueForCursor(manifest, current)"));
        assertTrue(shell.contains("restartActiveCueImmediatelyAtRate(activeCue.get(), normalized)"));
        assertTrue(shell.contains("playbackCursor.set(new PlaybackCursor(cue.segmentId(), cue.startSeconds(), false))"));
        assertTrue(shell.contains("playbackTransport.activateRuntimeCue(cue)"));
        assertTrue(shell.contains("playExactCue(cue, 0.0)"));
        assertFalse(shell.contains("playbackCursor.get() != null"));
    }

    @Test
    void playbackTickUsesTransitionGuardAndExplicitCueTransition() throws IOException {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String transport = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java");
        assertTrue(shell.contains("if (playbackTransport.sequentialActive()) { return; }"));
        assertTrue(shell.contains("playbackTransport.nextCue(manifest, completedCue)"));
        assertTrue(shell.contains("if (transitionToCue(next.get()))"));
        assertTrue(shell.contains("private boolean transitionToCue(PlaybackCue cue)"));
        assertTrue(transport.contains("player.stop();"));
        assertTrue(transport.contains("continuation.stop();"));
    }

    @Test
    void playFromSelectedSegmentRespectsDocumentSelectionInsteadOfFallingBackToFirstCue() throws IOException {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertTrue(shell.contains("Optional<NarrationSegment> segment = selectedDocumentSegmentOrSelected();"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
