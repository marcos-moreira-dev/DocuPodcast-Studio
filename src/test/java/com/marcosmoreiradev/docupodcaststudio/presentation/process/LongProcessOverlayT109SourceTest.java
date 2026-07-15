package com.marcosmoreiradev.docupodcaststudio.presentation.process;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LongProcessOverlayT109SourceTest {
    @Test
    void shellMountsProcessOverlayOnTopOfWorkspace() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String overlay = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/LongProcessOverlayView.java");
        String css = read("src/main/resources/css/components/process-overlay.css");
        String vm = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String selectionCoordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentSelectionCoordinator.java");

        assertTrue(shell.contains("new LongProcessOverlayView(viewModel, processOverlayExpanded)"));
        assertTrue(shell.contains("new StackPane(workspaceHost, processOverlay)"));
        assertTrue(shell.contains("Pos.BOTTOM_RIGHT"));
        assertTrue(overlay.contains("activeAudioJobStatusProperty"));
        assertTrue(overlay.contains("cancelActiveAudioJob"));
        assertTrue(overlay.contains("Ocultar"));
        assertTrue(shell.contains("new SimpleBooleanProperty(true)"));
        assertTrue(css.contains("process-overlay"));
        assertFalse(vm.contains("activeWorkspace.set(WorkspaceKind.AUDIO_JOBS)"));
        assertFalse(vm.contains("navigateToWorkspace(WorkspaceKind.AUDIO_JOBS)"));
    }

    @Test
    void rightRailHasNoAssignedSectionAndClickUpdatesContext() throws Exception {
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String vm = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String selectionCoordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentSelectionCoordinator.java");

        assertFalse(rail.contains("Asignadas"));
        assertFalse(rail.contains("renderAssignedMedia"));
        assertTrue(rail.contains("Fragmentos visuales"));
        assertFalse(rail.contains("railTitle(\"Imágenes\")"));
        assertTrue(rail.contains("selectDocumentFragmentRailItem"));
        assertTrue(vm.contains("selectDocumentRangeForSegment"));
        assertTrue(selectionCoordinator.contains("previewForRange"));
        assertTrue(vm.contains("selectedDocumentTextRange.set"));
        assertTrue(vm.contains("documentSelectionWorkflow"));
        assertTrue(vm.contains("bumpDocumentMediaRevision"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
