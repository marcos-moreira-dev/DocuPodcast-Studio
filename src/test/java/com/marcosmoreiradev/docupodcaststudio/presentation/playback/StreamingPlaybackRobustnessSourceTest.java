package com.marcosmoreiradev.docupodcaststudio.presentation.playback;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StreamingPlaybackRobustnessSourceTest {
    @Test
    void readerShowsBufferStateAndKeepsChunkFactoryBehindTheDocument() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackWorkflowCoordinator.java"));
        String window = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/playback/StreamingPlaybackWindow.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));

        assertTrue(window.contains("waitingForNextChunk"));
        assertTrue(window.contains("readerStatusLabel"));
        assertTrue(shell.contains("streamingBufferStatusProperty"));
        assertTrue(shell.contains("refreshStreamingBufferStatus"));
        assertTrue(shell.contains("streamingPlaybackWindow"));
        assertTrue(shell.contains("playbackWorkflow.waitingForBufferMessage"));
        assertTrue(workflow.contains("policy.waitingLabel"));
        assertTrue(document.contains("documentModeLabel"));
        assertFalse(document.contains("streamingBufferStatusProperty"));
        assertFalse(document.contains("document-listen-buffer-status"));
        assertTrue(css.contains("document-listen-buffer-status"));
        assertTrue(settings.contains("Oraciones antes de empezar")
                || settings.contains("Fragmentos listos antes de empezar"));
        assertTrue(settings.contains("Oraciones adelantadas")
                || settings.contains("Fragmentos adelantados"));
    }
}
