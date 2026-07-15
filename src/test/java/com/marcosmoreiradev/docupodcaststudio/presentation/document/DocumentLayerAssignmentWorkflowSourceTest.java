package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentLayerAssignmentWorkflowSourceTest {
    @Test
    void contextualInspectorPersistsAssignmentsAndSupportsRemoveOrSelect() throws Exception {
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String audioPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String imagePanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerCoordinator.java"));
        String mediaRail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String projection = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentLayerAssignmentPresentation.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"))
                + Files.readString(Path.of("src/main/resources/css/document-reader.css"));

        assertTrue(coordinator.contains("withNarrativeLayerAssignment(assignment)"));
        assertTrue(coordinator.contains("withoutNarrativeLayerAssignment"));
        assertTrue(coordinator.contains("removeFirstAssignmentOfKind"));
        assertTrue(viewModel.contains("narrativeLayerWorkflow.assign"));
        assertTrue(viewModel.contains("selectNarrativeLayerAssignment"));
        assertTrue(viewModel.contains("removeImageAssignmentForSelectedDocumentRange"));
        assertTrue(viewModel.contains("removeEmotionAssignmentForSelectedDocumentRange"));
        assertTrue(viewModel.contains("No se encontró un fragmento preparado"));
        assertTrue(coordinator.contains("Se guardó en el proyecto, no en el documento fuente"));
        assertTrue(coordinator.contains("NarrativeLayerAssignmentPolicy.conflicts"));

        assertTrue(audioPanel.contains("Quitar audio"));
        assertTrue(imagePanel.contains("Quitar imagen"));
        assertFalse(mediaRail.contains("Asignadas"));
        assertFalse(mediaRail.contains("renderAssignedMedia"));
        assertTrue(mediaRail.contains("Fragmentos visuales"));
        assertFalse(mediaRail.contains("railTitle(\"Imágenes\")"));
        assertTrue(mediaRail.contains("selectDocumentFragmentRailItem"));

        assertTrue(projection.contains("from(NarrativeLayerAssignment assignment)"));
        assertTrue(projection.contains("primaryLayer"));
        assertTrue(projection.contains("documentRange().displayLabel()"));

        assertTrue(css.contains("document-layer-assignment"));
        assertTrue(css.contains("document-layer-assignment-primary"));
        assertTrue(css.contains("document-context-module"));
    }
}
