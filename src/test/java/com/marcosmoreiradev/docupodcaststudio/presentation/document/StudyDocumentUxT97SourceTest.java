package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyDocumentUxT97SourceTest {
    @Test
    void ribbonOpensProblemDockWithoutActivatingSelection() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");

        assertTrue(shell.contains("viewModel.openTechnicalProblemPanel()"));
        assertTrue(viewModel.contains("public void openTechnicalProblemPanel()"));
        assertTrue(viewModel.contains("documentRightRailVisible.set(true)"));
        assertTrue(registry.contains("Abrir el panel de problema tecnico"));
    }

    @Test
    void rightProblemDockIsCompactAndUsesVerticalActions() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java");

        assertFalse(panel.contains("TextArea preview"));
        assertTrue(panel.contains("Seleccionar fragmentos para asociar a problema"));
        assertTrue(panel.contains("new VBox(8, clear, generate)"));
        assertTrue(panel.contains("Renderizar y exportar todos..."));
        assertTrue(panel.contains("new VBox(8, exportAllPng, exportAllPdf, editSaved, exportPng, exportText, deleteSaved)"));
        assertTrue(panel.contains("fullWidth(button)"));
    }

    @Test
    void sourceVisualImagesAreResponsiveAndCanvasIsTiled() throws Exception {
        String visual = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String canvas = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");

        assertTrue(visual.contains("ResponsiveSourceImageHost"));
        assertFalse(visual.contains("DEFAULT_IMAGE_FIT_WIDTH"));
        assertFalse(visual.contains("setFitHeight(480)"));
        assertTrue(dialog.contains("StudyProblemCanvasSurface"));
        assertTrue(canvas.contains("new Canvas(CANVAS_TILE_SIZE, CANVAS_TILE_SIZE)"));
        assertFalse(dialog.contains("new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT)"));
        assertTrue(dialog.contains("source.imageBase64()"));
        assertTrue(dialog.contains("Transferir imagen al lienzo"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
