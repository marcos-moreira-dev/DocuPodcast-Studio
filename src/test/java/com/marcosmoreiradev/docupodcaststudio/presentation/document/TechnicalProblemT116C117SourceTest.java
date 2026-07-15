package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemT116C117SourceTest {
    @Test
    void pdfCaptureUsesEventFiltersOnPageFrameAndOverlay() throws Exception {
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");

        assertTrue(pdfView.contains("selectionOverlay.addEventFilter(MouseEvent.MOUSE_PRESSED"));
        assertTrue(pdfView.contains("frame.addEventFilter(MouseEvent.MOUSE_PRESSED"));
        assertTrue(pdfView.contains("displayedImageBounds()"));
        assertFalse(pdfView.contains("if (event.isConsumed())"));
    }

    @Test
    void modalHasCanvasRegionSelectionAndStableGrowthGuard() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String surface = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");
        String css = read("src/main/resources/css/document/study-problem.css");

        assertTrue(dialog.contains("canvasRegionSelectionMode"));
        assertTrue(dialog.contains("\"Seleccionar region\""));
        assertTrue(dialog.contains("copyCanvasRegionSelection"));
        assertTrue(dialog.contains("deleteCanvasRegionSelection"));
        assertTrue(dialog.contains("moveCanvasRegionSelection"));
        assertTrue(dialog.contains("canvasGrowScheduled"));
        assertTrue(surface.contains("SCROLL_GROWTH_FACTOR = 1.25"));
        assertTrue(surface.contains("snapshotRegion"));
        assertTrue(surface.contains("eraseRegion"));
        assertTrue(css.contains(".technical-problem-statement-header-box"));
        assertTrue(css.contains("-fx-background-color: #ffffff"));
        assertTrue(css.contains("-fx-padding: 20 20 8 20"));
        assertTrue(css.contains(".technical-problem-canvas-region-selection"));
        assertFalse(dialog.contains("\"Cargar imagen externa...\""));
    }

    @Test
    void sideDockExportsSavedProblemsAsPdf() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/StudyProblemWorkflow.java");
        String exporter = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/document/PdfBoxStudyProblemPdfExporter.java");

        assertTrue(panel.contains("\"Exportar ejercicios en PDF...\""));
        assertTrue(panel.contains("exportAllProblemsPdf"));
        assertTrue(panel.contains("new FileChooser()"));
        assertTrue(viewModel.contains("exportAllTechnicalProblemImagesPdf"));
        assertTrue(workflow.contains("exportAllSolutionImagesAsPdf"));
        assertTrue(workflow.contains("StudyProblemPdfExporter"));
        assertTrue(exporter.contains("PDDocument"));
        assertTrue(exporter.contains("LosslessFactory.createFromImage"));
        assertTrue(exporter.contains("setNonStrokingColor(Color.WHITE)"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
