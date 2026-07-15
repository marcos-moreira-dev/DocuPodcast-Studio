package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PresentationCoordinatorsRf2SourceTest {
    @Test
    void shellViewModelDelegatesDocumentSelectionPresentationRules() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentSelectionCoordinator.java");
        assertTrue(viewModel.contains("DocumentSelectionCoordinator"));
        assertTrue(viewModel.contains("documentSelectionWorkflow.sentenceSelection"));
        assertTrue(viewModel.contains("documentSelectionWorkflow.documentRangeSelection"));
        assertTrue(viewModel.contains("documentSelectionWorkflow.primaryAction"));
        assertTrue(viewModel.contains("applySelectionLabels(DocumentSelectionCoordinator.SelectionLabels"));
        assertTrue(coordinator.contains("record SelectionLabels"));
        assertTrue(coordinator.contains("record PrimaryAction"));
        assertTrue(coordinator.contains("sourceLocationLabel"));
        assertTrue(coordinator.contains("previewForRange"));
    }

    @Test
    void documentSelectionCoordinatorOwnsReaderActionCopy() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentSelectionCoordinator.java");
        assertFalse(viewModel.contains("Una acción principal: prepara la lectura, genera audio si hace falta o reproduce cuando ya exista manifest."));
        assertTrue(coordinator.contains("Una acción principal: prepara la lectura, genera audio si hace falta o reproduce cuando ya exista manifest."));
        assertTrue(coordinator.contains("Reproducir selección"));
        assertTrue(coordinator.contains("Reproducir desde aquí"));
        assertTrue(coordinator.contains("Escuchar documento"));
    }

    @Test
    void shellViewModelKeepsShrinkingAfterRf2() throws Exception {
        long lines = Files.lines(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java")).count();
        assertTrue(lines <= 2700, "DocuPodcastShellViewModel should stay below 2700 lines until RF-TX1, actual: " + lines);
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
