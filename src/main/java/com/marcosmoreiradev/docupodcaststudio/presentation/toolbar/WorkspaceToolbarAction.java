package com.marcosmoreiradev.docupodcaststudio.presentation.toolbar;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceCapability;

import java.util.Objects;

/** Contextual toolbar action derived from a workspace capability. */
public record WorkspaceToolbarAction(String group, String label, WorkspaceCapability capability, AppCommandId commandId, boolean primary) {
    public WorkspaceToolbarAction {
        group = requireText(group, "group");
        label = requireText(label, "label");
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(commandId, "commandId");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacio.");
        }
        return value.strip();
    }
}
