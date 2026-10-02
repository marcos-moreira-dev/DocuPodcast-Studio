package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

import javafx.scene.Group;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WorkspaceViewRegistryValidationTest {
    @Test
    void implementedWorkspaceWithoutFactoryOrDelegationFailsFast() {
        WorkspaceViewRegistry registry = new WorkspaceViewRegistry(WorkspaceDescriptorCatalog.official())
                .register(WorkspaceKind.WELCOME_HOME, Group::new);
        assertThrows(IllegalStateException.class, registry::validateRegistrations);
    }

    @Test
    void narrativeDelegationIsAnExplicitValidComposition() {
        WorkspaceViewRegistry registry = new WorkspaceViewRegistry(WorkspaceDescriptorCatalog.official())
                .register(WorkspaceKind.WELCOME_HOME, Group::new)
                .register(WorkspaceKind.DOCUMENT_READER, Group::new)
                .registerAlias(WorkspaceKind.NARRATIVE_VISUAL_PRODUCTION, WorkspaceKind.DOCUMENT_READER)
                .register(WorkspaceKind.THEATRE_SCRIPT, Group::new)
                .register(WorkspaceKind.VOICE_LIBRARY, Group::new)
                .register(WorkspaceKind.THEATRE_IMAGE_GENERATION, Group::new);
        assertDoesNotThrow(registry::validateRegistrations);
    }
}
