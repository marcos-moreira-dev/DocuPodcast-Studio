package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfRegionTechnicalProblemT103T104SourceTest {
    @Test
    void pdfViewerSupportsRectangleSelectionWithoutPdfBoxInPresentation() throws Exception {
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/DocumentApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");

        assertTrue(pdfView.contains("setRegionSelectionActive"));
        assertTrue(pdfView.contains("selectionOverlay"));
        assertTrue(pdfView.contains("imagePointFromOverlay"));
        assertTrue(pdfView.contains("pdf-visual-region-selection"));
        assertTrue(pdfView.contains("MIN_REGION_SELECTION_SIZE"));
        assertTrue(pdfView.contains("new PdfViewportSelection"));
        assertTrue(workspace.contains("capturePdfRegionSelection"));
        assertTrue(workspace.contains("new PdfRegionCaptureRequest"));
        assertTrue(workspace.contains("capturePdfVisualRegion().capture"));
        assertTrue(services.contains("CapturePdfVisualRegionUseCase capturePdfVisualRegion"));
        assertTrue(factory.contains("new CapturePdfVisualRegionUseCase(pdfRenderEngine)"));
        assertFalse(pdfView.contains("PdfBoxRenderEngine"));
        assertFalse(workspace.contains("PdfBoxRenderEngine"));
    }

    @Test
    void pdfProblemsUseRegionCapturesAndNonPdfKeepsCheckboxBlocks() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java");
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/StudyProblemWorkflow.java");

        assertTrue(workspace.contains("ObservableList<PdfRegionCaptureDraft> pdfRegionCaptureSelection"));
        assertTrue(workspace.contains("showForDrafts"));
        assertTrue(workspace.contains("saveTechnicalProblemFromSources"));
        assertTrue(workspace.contains("prepareStudySourceCrops().prepare(document, selectedBlocks)"));
        assertTrue(workspace.contains("problemCheckBox"));
        assertTrue(panel.contains("Seleccionar capturas del PDF para asociar a problema"));
        assertTrue(panel.contains("currentDocumentIsPdf"));
        assertTrue(panel.contains("selectedPdfRegions"));
        assertTrue(dialog.contains("showForDrafts"));
        assertTrue(dialog.contains("statementSourcesFromDrafts"));
        assertTrue(workflow.contains("saveFromSources"));
        assertTrue(workflow.contains("StudySourceReference.visualRegion"));
        assertTrue(workflow.contains("StudySourceReference.fullBlock"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
