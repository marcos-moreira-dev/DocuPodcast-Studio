package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductCleanNavigationT114Hf1SourceTest {
    @Test
    void shellDoesNotActivateLegacyWorkspacesAfterPreparingReading() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertFalse(shell.contains("activeWorkspace.set(WorkspaceKind.SCRIPT_EDITOR)"));
        assertFalse(shell.contains("activeWorkspace.set(WorkspaceKind.STORYBOARD)"));
        assertFalse(shell.contains("navigateToWorkspace(WorkspaceKind.SCRIPT_EDITOR)"));
        assertFalse(shell.contains("navigateToWorkspace(WorkspaceKind.STORYBOARD)"));
        assertFalse(shell.contains("withViewState(\"activeWorkspace\", WorkspaceKind.SCRIPT_EDITOR.name())"));
        assertFalse(shell.contains("withViewState(\"activeWorkspace\", WorkspaceKind.STORYBOARD.name())"));
        assertTrue(shell.contains("withViewState(\"activeWorkspace\", WorkspaceKind.DOCUMENT_READER.name())"));
    }

    @Test
    void presentationSurfaceDoesNotExposeGuionAsProductLabel() throws Exception {
        String workspaceKind = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceKind.java"));
        String descriptorCatalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceDescriptorCatalog.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertFalse(workspaceKind.contains("SCRIPT_EDITOR(\"Guion\")"));
        assertFalse(descriptorCatalog.contains("\"Guion\""));
        assertFalse(shell.contains("guion narrable"));
        assertFalse(shell.contains("Workspace Guion"));
        assertTrue(shell.contains("prepara la lectura") || shell.contains("preparará la lectura"));
    }
}
