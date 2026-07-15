package com.marcosmoreiradev.docupodcaststudio.presentation.command;

import com.marcosmoreiradev.docupodcaststudio.presentation.toolbar.WorkspaceToolbarAction;
import com.marcosmoreiradev.docupodcaststudio.presentation.toolbar.WorkspaceToolbarActionProvider;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorkspaceCapabilityCommandMapperTest {
    @Test
    void everyContextualToolbarActionCarriesRegisteredCommandId() {
        AppCommandRegistry registry = AppCommandRegistry.official();
        WorkspaceToolbarActionProvider provider = WorkspaceToolbarActionProvider.official();
        for (WorkspaceKind kind : WorkspaceKind.values()) {
            for (WorkspaceToolbarAction action : provider.actionsFor(kind)) {
                assertTrue(registry.contains(action.commandId()), kind + " no tiene comando registrado para " + action.capability());
            }
        }
    }
}
