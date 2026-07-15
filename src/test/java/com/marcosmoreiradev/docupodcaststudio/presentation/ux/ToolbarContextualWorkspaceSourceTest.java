package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ToolbarContextualWorkspaceSourceTest {
    @Test
    void toolbarShouldRefreshContextualActionsFromProductWorkspaceActionProvider() throws Exception {
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String provider = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/CommandAvailabilityPolicy.java"));
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceDescriptorCatalog.java"));
        String routeResolver = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceSurfacePolicy.java"));

        assertTrue(toolbar.contains("activeWorkspaceProperty().addListener"));
        assertTrue(toolbar.contains("refreshContextualRow"));
        assertTrue(toolbar.contains("WorkspaceToolbarActionProvider"));
        assertTrue(toolbar.contains("WorkspaceCapabilityPolicy"));
        assertTrue(toolbar.contains("actionProvider.actionsFor(workspaceKind)"));
        assertTrue(provider.contains("case DOCUMENT_READER"));
        assertTrue(provider.contains("case VOICE_LIBRARY"));
        assertTrue(provider.contains("case WELCOME_HOME"));
        assertFalse(provider.contains("case SCRIPT_EDITOR"),
                "El provider contextual no debe volver a tratar Guion como workspace visual de producto.");
        assertFalse(provider.contains("case STORYBOARD"),
                "El storyboard vive como rail/capa del Documento, no como fila contextual propia.");
        assertFalse(provider.contains("case AUDIO_JOBS"),
                "Los jobs de audio deben migrar a overlay/proceso, no a toolbar contextual propia.");
        assertTrue(catalog.contains("put(descriptors, WorkspaceKind.SCRIPT_EDITOR"));
        assertTrue(catalog.contains("put(descriptors, WorkspaceKind.AUDIO_JOBS"));
        assertTrue(catalog.contains("put(descriptors, WorkspaceKind.STORYBOARD"));
        assertTrue(routeResolver.contains("DOCUMENT_READER"));
        assertTrue(policy.contains("LISTEN_DOCUMENT -> viewModel.currentDocumentProperty().isNull()"));
        assertTrue(policy.contains("PREPARE_DOCUMENT_READING"));
        assertTrue(toolbar.contains("setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)"));
        assertFalse(toolbar.contains("Whisper/STT"));
        assertFalse(toolbar.contains("Play selección"));
    }
}
