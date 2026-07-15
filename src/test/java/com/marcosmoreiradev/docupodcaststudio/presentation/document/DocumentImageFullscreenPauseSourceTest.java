package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentImageFullscreenPauseSourceTest {
    @Test
    void sourceDocumentImagesExplainAndCoordinateFullscreenPlaybackPause() throws Exception {
        String sourceVisual = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java"));
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String imagePanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String viewer = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ImageFullscreenViewer.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(sourceVisual.contains("Pantalla completa (se pausa la reproduccion)"));
        assertTrue(sourceVisual.contains("Presiona Escape para salir y reanudar la reproduccion de la narracion."));
        assertTrue(sourceVisual.contains("pausedByFullscreen"));
        assertTrue(workspace.contains("viewModel::playbackActiveForFullscreenPause"));
        assertTrue(workspace.contains("viewModel::pausePlayback"));
        assertTrue(workspace.contains("viewModel::resumePlayback"));

        assertTrue(imagePanel.contains("Pantalla completa (se pausa la reproduccion)"));
        assertTrue(imagePanel.contains("viewModel.playbackActiveForFullscreenPause()"));
        assertTrue(imagePanel.contains("viewModel.pausePlayback()"));
        assertTrue(imagePanel.contains("viewModel.resumePlayback()"));
        assertTrue(viewModel.contains("playbackTransport.playerPlaying()"));
        assertTrue(viewModel.contains("playbackTransport.continuationActive()"));
        assertTrue(viewModel.contains("playbackTransport.sequentialActive()"));

        assertTrue(viewer.contains("Runnable beforeShow"));
        assertTrue(viewer.contains("Runnable afterClose"));
        assertTrue(viewer.contains("stage.setOnHidden"));
        assertTrue(viewer.contains("No se pudo reanudar la lectura"));
    }
}
