package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrativeVideoTanda4SourceTest {
    @Test
    void narrativeVideoCommandsAreRegisteredVisibleByModeAndDoNotReopenStoryboardWorkspace() throws Exception {
        String ids = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java"));
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertTrue(ids.contains("IMPORT_NARRATIVE_VIDEO_GRAMMAR"));
        assertTrue(ids.contains("EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE"));
        assertTrue(ids.contains("IMPORT_BRIDGE_IMAGE_FOR_SELECTION"));
        assertTrue(registry.contains("Elegir imagen puente"));
        assertTrue(policy.contains("ProjectMode.NARRATIVE_VIDEO"));
        assertTrue(policy.contains("capabilities.narrativeVisuals()"));
        assertTrue(ribbon.contains("\"Video narrativo\""));
        assertTrue(shell.contains("handleImportNarrativeVideoGrammar"));
        assertTrue(shell.contains("handleImportBridgeImageForSelection"));
        assertFalse(ribbon.contains("cmd(AppCommandId.OPEN_STORYBOARD"));
        assertFalse(shell.contains("register(AppCommandId.OPEN_STORYBOARD"));
    }

    @Test
    void shellViewModelStaysWithinBudgetAndBridgeImageDoesNotRefreshLegacyStoryboard() throws Exception {
        Path viewModelPath = Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String viewModel = Files.readString(viewModelPath);
        String workflow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeImageGenerationWorkflow.java"));
        long lines = Files.readAllLines(viewModelPath).size();

        assertTrue(lines <= 2850, "DocuPodcastShellViewModel debe mantenerse acotado aunque exponga acciones narrativas por fragmento");
        assertTrue(viewModel.contains("generateNarrativeImageForSegment(String segmentId)"));
        assertTrue(viewModel.contains("importImageForSegment(String segmentId, Path imageFile, NarrativeLayerKind imageKind)"));
        assertTrue(viewModel.contains("removeImageAssignmentForSegment(String segmentId, NarrativeLayerKind imageKind)"));
        assertTrue(viewModel.contains("NarrativeImageGenerationWorkflow"));
        assertTrue(workflow.contains("ComfyUiVisualEngineClient"));
        assertTrue(workflow.contains("VisualEngineRequest"));
        assertTrue(workflow.contains("ImageEnhancementOutputProfile.FHD_1080"));
        assertTrue(viewModel.contains("NarrativeLayerKind.BRIDGE_IMAGE"));
        assertTrue(viewModel.contains("if (normalizedKind == NarrativeLayerKind.IMAGE) refreshStoryboardFromImageLayers(session)"));
    }

    @Test
    void narrativeVisualWorkspaceIsOperationalPerFragment() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/narrative/NarrativeVisualProductionWorkspaceView.java"));

        assertTrue(workspace.contains("MediaThumbnailCard"));
        assertTrue(workspace.contains("ActionButtonFactory"));
        assertTrue(workspace.contains("\"Elegir imagen\""));
        assertTrue(workspace.contains("\"Generar con IA\""));
        assertTrue(workspace.contains("\"Elegir puente\""));
        assertTrue(workspace.contains("\"Quitar\""));
        assertTrue(workspace.contains("\"Pantalla completa\""));
        assertTrue(workspace.contains("viewModel.importImageForSegment(fragment.segmentId()"));
        assertTrue(workspace.contains("viewModel.generateNarrativeImageForSegment(fragment.segmentId())"));
        assertTrue(workspace.contains("viewModel.removeImageAssignmentForSegment(fragment.segmentId()"));
    }
}
