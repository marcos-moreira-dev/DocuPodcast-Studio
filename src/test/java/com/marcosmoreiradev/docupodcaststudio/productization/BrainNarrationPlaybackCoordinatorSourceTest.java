package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrainNarrationPlaybackCoordinatorSourceTest {
    @Test
    void narrationAndPlaybackBrainMovesIntoDedicatedCoordinators() throws Exception {
        String narration = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentNarrationCoordinator.java");
        String playback = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackWorkflowCoordinator.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(narration.contains("document-root prepared-reading brain"));
        assertTrue(narration.contains("internal compatibility payload"));
        assertTrue(narration.contains("readinessProblem"));
        assertTrue(narration.contains("buildNarrationProjection"));

        assertTrue(playback.contains("document-root playback decisions"));
        assertTrue(playback.contains("buffered playback"));
        assertTrue(playback.contains("preferredStartCue"));
        assertTrue(playback.contains("nextCueAfterGap"));

        assertTrue(shell.contains("new DocumentNarrationCoordinator"));
        assertTrue(shell.contains("new PlaybackWorkflowCoordinator"));
        assertTrue(shell.contains("documentNarration.buildNarrationProjection"));
        assertTrue(shell.contains("playbackWorkflow.canStartBufferedPlayback"));
        assertTrue(shell.contains("playbackWorkflow.nextCueAfterGap"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
