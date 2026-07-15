package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentImageContextPanelSourceTest {
    @Test
    void imageLayerExposesTheatreCameraCatalogAndStageBackdropControls() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String viewModel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(source.contains("Tipo de plano"));
        assertTrue(source.contains("Aplicar plano"));
        assertTrue(source.contains("setSelectedTheatreApplyCameraPlane"));
        assertTrue(source.contains("cameraSelectionNote"));
        assertTrue(source.contains("Plano de este fragmento visual: "));
        assertTrue(source.contains("ComboBox<CameraOption> cameraCombo"));
        assertTrue(source.contains("StudioFormControls.combo"));
        assertTrue(source.contains("Catalogo de tipos de plano"));
        assertTrue(source.contains("theatre-camera-catalog-dialog"));
        assertTrue(source.contains("theatre-camera-catalog-card"));
        assertTrue(source.contains("stage.setFullScreen(true)"));
        assertTrue(source.contains("KeyCode.ESCAPE"));
        assertFalse(source.contains("Importar catalogo de planos"));
        assertTrue(source.contains("Fondo de escenario"));
        assertTrue(source.contains("Fondo no asignado\\n(se aplica por defecto)"));
        assertTrue(source.contains("Quitar fondo a partir de esta imagen"));
        assertTrue(source.contains("viewModel.setTheatreCameraCueForSelectedSegment"));
        assertTrue(source.contains("refreshTheatreVisualControls"));
        assertTrue(source.contains("MouseButton.PRIMARY"));
        assertTrue(source.contains("viewModel.assignStageBackdropForSelectedSegment"));
        assertTrue(source.contains("viewModel.selectedTheatreStageBackdropPreview"));
        assertTrue(source.contains("viewModel.clearStageBackdropFromSelectedSegment"));
        assertTrue(source.contains("viewModel.selectedScriptSegmentIdProperty()"));
        assertTrue(source.contains("viewModel.selectedVisualFragmentSegmentIdProperty()"));
        assertTrue(viewModel.contains("selectedVisualFragmentSegmentId"));
        assertTrue(viewModel.contains("selectedTheatreVisualSegment() { return findSegment(selectedVisualFragmentSegmentId.get()).or(() -> findSegment(selectedScriptSegmentId.get())).or(() -> firstSegmentForDocumentBlock(selectedDocumentBlockId.get()))"));
        int clearStart = viewModel.indexOf("private void clearDocumentTextRange()");
        int clearEnd = viewModel.indexOf("private void applySelectionLabels", clearStart);
        String clearMethod = viewModel.substring(clearStart, clearEnd);
        assertFalse(clearMethod.contains("selectedVisualFragmentImageUri.set"));
    }
}
