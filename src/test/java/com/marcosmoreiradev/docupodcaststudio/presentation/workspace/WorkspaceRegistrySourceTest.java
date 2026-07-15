package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkspaceRegistrySourceTest {
    @Test
    void shellUsesRegistryAndRouteResolverInsteadOfManualWorkspaceMap() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceViewRegistry.java"));

        assertTrue(shell.contains("WorkspaceDescriptorCatalog.official()"));
        assertTrue(shell.contains("WorkspaceRouteResolver"));
        assertTrue(shell.contains("WorkspaceViewRegistry"));
        assertTrue(shell.contains("workspaceRegistry.viewFor(resolved)"));
        assertFalse(shell.contains("new EnumMap<WorkspaceKind, Node>"));
        assertFalse(shell.contains("workspaces.getOrDefault"));
        assertTrue(registry.contains("PlaceholderWorkspaceView"));
    }
}
