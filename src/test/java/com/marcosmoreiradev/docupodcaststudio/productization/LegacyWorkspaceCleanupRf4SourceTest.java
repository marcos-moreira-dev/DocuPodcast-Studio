package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guardrail for RF4: legacy workspaces remain code-only compatibility surfaces, not GUI routes. */
final class LegacyWorkspaceCleanupRf4SourceTest {
    @Test
    void workspaceRegistryRejectsLegacyFactoriesAndRoutesRequestsToProductSurface() throws Exception {
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceViewRegistry.java");
        assertTrue(registry.contains("WorkspaceSurfacePolicy surfacePolicy"));
        assertTrue(registry.contains("!surfacePolicy.isPrimarySurface(resolved)"));
        assertTrue(registry.contains("Workspace heredado no registrable en la superficie principal"));
        assertTrue(registry.contains("surfacePolicy.restoreStartupWorkspace(catalog.descriptor(kind).kind())"));
    }

    @Test
    void shellViewModelNoLongerExposesOpenLegacyWorkspaceMethods() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        assertFalse(viewModel.contains("showScriptWorkspace()"));
        assertFalse(viewModel.contains("showStoryboardWorkspace()"));
        assertFalse(viewModel.contains("showAudioWorkspace()"));
        assertFalse(viewModel.contains("activeWorkspace.set(WorkspaceKind.SCRIPT_EDITOR"));
        assertFalse(viewModel.contains("activeWorkspace.set(WorkspaceKind.AUDIO_JOBS"));
        assertFalse(viewModel.contains("activeWorkspace.set(WorkspaceKind.STORYBOARD"));
    }

    @Test
    void shellDoesNotRegisterHandlersForHiddenLegacyWorkspaceCommands() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        assertFalse(shell.contains("register(AppCommandId.OPEN_STORYBOARD"));
        assertFalse(shell.contains("register(AppCommandId.OPEN_AUDIO_JOBS"));
        assertFalse(shell.contains("showStoryboardWorkspace"));
        assertFalse(shell.contains("showAudioWorkspace"));
        assertFalse(shell.contains("showScriptWorkspace"));
    }

    @Test
    void commandRegistryKeepsLegacyOpenCommandsHiddenOnly() throws Exception {
        String registry = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java");
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_STORYBOARD"));
        assertTrue(registry.contains("hidden(AppCommandId.OPEN_AUDIO_JOBS"));
        assertFalse(registry.contains("command(AppCommandId.OPEN_STORYBOARD"));
        assertFalse(registry.contains("command(AppCommandId.OPEN_AUDIO_JOBS"));
    }

    @Test
    void documentationMarksLegacyWorkspaceCleanupAsImplemented() throws Exception {
        String readme = read("README.md");
        String handoff = read("AI_HANDOFF.md");
        String validation = read("VALIDATION.md");
        assertTrue(readme.contains("RF4 — Limpieza de workspaces heredados"));
        assertTrue(handoff.contains("RF4 — Limpieza de workspaces heredados"));
        assertTrue(validation.contains("RF4 — Limpieza de workspaces heredados"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
