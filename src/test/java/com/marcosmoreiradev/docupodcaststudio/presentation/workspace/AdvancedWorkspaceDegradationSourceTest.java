package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T99A guardrail: legacy views are removed from normal navigation before strong GUI redesign. */
final class AdvancedWorkspaceDegradationSourceTest {
    @Test
    void onlyInicioDocumentoVocesAndTeatroAreProductWorkspaces() {
        WorkspaceDescriptorCatalog catalog = WorkspaceDescriptorCatalog.official();

        assertTrue(catalog.descriptor(WorkspaceKind.WELCOME_HOME).primaryNavigation());
        assertTrue(catalog.descriptor(WorkspaceKind.DOCUMENT_READER).primaryNavigation());
        assertTrue(catalog.descriptor(WorkspaceKind.VOICE_LIBRARY).primaryNavigation());
        assertTrue(catalog.descriptor(WorkspaceKind.THEATRE_SCRIPT).primaryNavigation());
        assertTrue(catalog.descriptor(WorkspaceKind.THEATRE_SCRIPT).implemented());

        assertFalse(catalog.descriptor(WorkspaceKind.SCRIPT_EDITOR).implemented());
        assertFalse(catalog.descriptor(WorkspaceKind.SCRIPT_EDITOR).primaryNavigation());
        assertFalse(catalog.descriptor(WorkspaceKind.AUDIO_JOBS).implemented());
        assertFalse(catalog.descriptor(WorkspaceKind.AUDIO_JOBS).primaryNavigation());
        assertFalse(catalog.descriptor(WorkspaceKind.STORYBOARD).implemented());
        assertFalse(catalog.descriptor(WorkspaceKind.STORYBOARD).primaryNavigation());
    }

    @Test
    void persistedLegacyWorkspaceRestoresToDocumentReaderButVoicesAndTheatreSurvive() {
        WorkspaceRouteResolver resolver = new WorkspaceRouteResolver(WorkspaceDescriptorCatalog.official());

        assertTrue(resolver.resolvePersisted("SCRIPT_EDITOR") == WorkspaceKind.DOCUMENT_READER);
        assertTrue(resolver.resolvePersisted("AUDIO_JOBS") == WorkspaceKind.DOCUMENT_READER);
        assertTrue(resolver.resolvePersisted("STORYBOARD") == WorkspaceKind.DOCUMENT_READER);
        assertTrue(resolver.resolvePersisted("VOICE_LIBRARY") == WorkspaceKind.VOICE_LIBRARY);
        assertTrue(resolver.resolvePersisted("THEATRE_SCRIPT") == WorkspaceKind.THEATRE_SCRIPT);
    }

    @Test
    void shellDoesNotExposeLegacyWorkspacesAsVisibleMenuItems() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));

        assertFalse(shell.contains("Narración interna"));
        assertFalse(shell.contains("Jobs de audio"));
        assertFalse(shell.contains("Storyboard vivo"));
        assertFalse(shell.contains("Audio a texto"));
        assertFalse(shell.contains("(avanzado)"));
    }
}
