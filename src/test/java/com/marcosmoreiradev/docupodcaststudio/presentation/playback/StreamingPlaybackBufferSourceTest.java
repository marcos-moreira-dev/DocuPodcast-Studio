package com.marcosmoreiradev.docupodcaststudio.presentation.playback;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StreamingPlaybackBufferSourceTest {
    @Test
    void shellStartsPlaybackWhenInitialBufferIsReadyAndContinuesAfterGaps() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackWorkflowCoordinator.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/playback/PlaybackBufferPolicy.java"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));

        assertTrue(policy.contains("initialReadySegments"));
        assertTrue(policy.contains("lookaheadSegments"));
        assertTrue(policy.contains("canStart"));
        assertTrue(shell.contains("playbackBufferPolicy"));
        assertTrue(shell.contains("documentPlaybackRequested"));
        assertTrue(shell.contains("waitingForBufferedSegmentAfter"));
        assertTrue(shell.contains("tryStartBufferedPlayback"));
        assertTrue(shell.contains("tryContinueAfterBufferGap"));
        assertTrue(shell.contains("waitForBufferedContinuation"));
        assertTrue(shell.contains("Audio inicial listo") || shell.contains("Reproduciendo con buffer"));
        assertTrue(policy.contains("Preparando el siguiente fragmento de audio"));
        assertTrue(shell.contains("playbackWorkflow.waitingForBufferMessage"));
        assertTrue(workflow.contains("policy.waitingLabel()"));
        assertTrue(settings.contains("Tanda 39: el lector puede iniciar cuando el buffer inicial está listo")
                || settings.contains("Fragmentos listos antes de empezar"));
    }
}
