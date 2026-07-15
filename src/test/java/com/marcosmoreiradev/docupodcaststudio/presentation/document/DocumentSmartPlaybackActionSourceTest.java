package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSmartPlaybackActionSourceTest {
    @Test
    void smartActionUsesDocumentSelectionWithoutAddingMoreButtons() throws Exception {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String floating = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));

        assertTrue(viewModel.contains("selectedDocumentBlockId"));
        assertTrue(viewModel.contains("documentPrimaryActionLabel"));
        assertTrue(viewModel.contains("documentPrimaryActionHint"));
        assertTrue(viewModel.contains("La elección real de voz") || viewModel.contains("sin modificar el Word original"));
        assertTrue(viewModel.contains("Escuchar documento"));
        assertTrue(viewModel.contains("Reproducir desde aquí"));
        assertTrue(viewModel.contains("selectedDocumentSegmentOrSelected"));
        assertTrue(viewModel.contains("manifest.cueForSegment(linked.get().id())"));
        assertTrue(document.contains("selectedDocumentBlockIdProperty"));
        assertTrue(document.contains("viewModel.selectDocumentBlock"));
        assertTrue(document.contains("clearSelectionFromBlankDocumentClick"));
        assertTrue(document.contains("stoppedState()"));
        assertTrue(toolbar.contains("viewModel.documentPrimaryActionLabelProperty()")
                || document.contains("new FloatingReadingControlBar"));
        assertTrue(toolbar.contains("ToolbarActionButton"));
        assertTrue(floating.contains("PrimaryActionStrip"));
        assertTrue(floating.contains("TransportControls"));
    }
}
