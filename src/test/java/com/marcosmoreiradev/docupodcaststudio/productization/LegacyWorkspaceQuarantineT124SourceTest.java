package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrail for T124: legacy implementation workspaces may exist, but they are not product surfaces. */
final class LegacyWorkspaceQuarantineT124SourceTest {
    @Test
    void shellRegistersOnlyProductWorkspaces() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        assertTrue(shell.contains("register(WorkspaceKind.WELCOME_HOME"));
        assertTrue(shell.contains("register(WorkspaceKind.DOCUMENT_READER"));
        assertTrue(shell.contains("register(WorkspaceKind.VOICE_LIBRARY"));
        assertFalse(shell.contains("register(WorkspaceKind.SCRIPT_EDITOR"));
        assertFalse(shell.contains("register(WorkspaceKind.AUDIO_JOBS"));
        assertFalse(shell.contains("register(WorkspaceKind.STORYBOARD"));
    }

    @Test
    void navigationNormalizesLegacySurfacesToDocument() throws Exception {
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceSurfacePolicy.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/WorkspaceNavigationCoordinator.java");
        assertTrue(policy.contains("LEGACY_INTERNAL_SURFACES"));
        assertTrue(policy.contains("WorkspaceKind.SCRIPT_EDITOR"));
        assertTrue(policy.contains("WorkspaceKind.AUDIO_JOBS"));
        assertTrue(policy.contains("WorkspaceKind.STORYBOARD"));
        assertTrue(policy.contains("return WorkspaceKind.DOCUMENT_READER;"));
        assertTrue(coordinator.contains("surfacePolicy.restoreStartupWorkspace(requestedWorkspace)"));
    }

    @Test
    void shellViewModelDoesNotActivateLegacyWorkspacesDirectly() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertFalse(viewModel.contains("activeWorkspace.set(WorkspaceKind.SCRIPT_EDITOR"));
        assertFalse(viewModel.contains("activeWorkspace.set(WorkspaceKind.AUDIO_JOBS"));
        assertFalse(viewModel.contains("activeWorkspace.set(WorkspaceKind.STORYBOARD"));
        assertFalse(viewModel.contains("withViewState(\"activeWorkspace\", WorkspaceKind.SCRIPT_EDITOR.name())"));
        assertFalse(viewModel.contains("withViewState(\"activeWorkspace\", WorkspaceKind.AUDIO_JOBS.name())"));
        assertFalse(viewModel.contains("withViewState(\"activeWorkspace\", WorkspaceKind.STORYBOARD.name())"));
    }

    @Test
    void legacyDescriptorsAreNotPrimaryNavigation() throws Exception {
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceDescriptorCatalog.java");
        assertTrue(catalog.contains("WorkspaceKind.SCRIPT_EDITOR"));
        assertTrue(catalog.contains("WorkspaceKind.AUDIO_JOBS"));
        assertTrue(catalog.contains("WorkspaceKind.STORYBOARD"));
        assertTrue(catalog.contains("Superficie interna heredada"));
        assertTrue(catalog.contains("false, false"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
