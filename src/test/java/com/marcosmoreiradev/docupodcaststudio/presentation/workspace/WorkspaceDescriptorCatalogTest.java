package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkspaceDescriptorCatalogTest {
    @Test
    void officialCatalogDefinesOnlyProductWorkspacesAsNavigationSurfaces() {
        WorkspaceDescriptorCatalog catalog = WorkspaceDescriptorCatalog.official();

        WorkspaceDescriptor welcome = catalog.descriptor(WorkspaceKind.WELCOME_HOME);
        assertTrue(welcome.implemented());
        assertTrue(welcome.primaryNavigation());

        WorkspaceDescriptor document = catalog.descriptor(WorkspaceKind.DOCUMENT_READER);
        assertTrue(document.implemented());
        assertTrue(document.primaryNavigation());
        assertTrue(document.supports(WorkspaceCapability.LISTEN_DOCUMENT));
        assertTrue(document.supports(WorkspaceCapability.CREATE_SCRIPT));

        WorkspaceDescriptor voices = catalog.descriptor(WorkspaceKind.VOICE_LIBRARY);
        assertTrue(voices.implemented());
        assertTrue(voices.primaryNavigation(), "Voces queda como workspace secundario real.");
        assertTrue(voices.supports(WorkspaceCapability.IMPORT_VOICE_SAMPLE));

        WorkspaceDescriptor theatreScript = catalog.descriptor(WorkspaceKind.THEATRE_SCRIPT);
        assertTrue(theatreScript.implemented());
        assertTrue(theatreScript.primaryNavigation(), "Teatro/Guion queda como superficie productiva real.");
        assertTrue(theatreScript.supports(WorkspaceCapability.LISTEN_DOCUMENT));
        assertTrue(theatreScript.supports(WorkspaceCapability.CREATE_SCRIPT));

        WorkspaceDescriptor audio = catalog.descriptor(WorkspaceKind.AUDIO_JOBS);
        assertFalse(audio.implemented(), "Audio Jobs deja de ser workspace productivo.");
        assertFalse(audio.primaryNavigation());

        WorkspaceDescriptor observability = catalog.descriptor(WorkspaceKind.OBSERVABILITY);
        assertFalse(observability.implemented(), "Observabilidad no debe presentarse como workspace productivo todavia.");
        assertFalse(observability.primaryNavigation());
    }

    @Test
    void routeResolverFallsBackToSafeProductSurfaces() {
        WorkspaceRouteResolver resolver = new WorkspaceRouteResolver(WorkspaceDescriptorCatalog.official());
        assertTrue(resolver.resolve(null) == WorkspaceKind.WELCOME_HOME);
        assertTrue(resolver.resolvePersisted("SCRIPT_EDITOR") == WorkspaceKind.DOCUMENT_READER);
        assertTrue(resolver.resolvePersisted("AUDIO_JOBS") == WorkspaceKind.DOCUMENT_READER);
        assertTrue(resolver.resolvePersisted("STORYBOARD") == WorkspaceKind.DOCUMENT_READER);
        assertTrue(resolver.resolvePersisted("THEATRE_SCRIPT") == WorkspaceKind.THEATRE_SCRIPT);
        assertTrue(resolver.resolvePersisted("VOICE_LIBRARY") == WorkspaceKind.VOICE_LIBRARY);
        assertTrue(resolver.resolvePersisted("NO_EXISTE") == WorkspaceKind.WELCOME_HOME);
    }
}
