package com.marcosmoreiradev.docupodcaststudio.presentation.compatibility;

import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;

import java.util.Set;

/** Read-only bridge for workspace identifiers persisted by historical builds. */
public final class LegacyWorkspaceRouteMapper {
    private static final Set<WorkspaceKind> HISTORICAL_INTERNAL = Set.of(
            WorkspaceKind.SCRIPT_EDITOR, WorkspaceKind.AUDIO_JOBS, WorkspaceKind.STORYBOARD);

    public boolean isHistoricalInternal(WorkspaceKind kind) {
        return kind != null && HISTORICAL_INTERNAL.contains(kind);
    }

    public WorkspaceKind productDestination(WorkspaceKind kind) {
        return isHistoricalInternal(kind) ? WorkspaceKind.DOCUMENT_READER : WorkspaceKind.WELCOME_HOME;
    }

    public Set<WorkspaceKind> historicalInternalKinds() {
        return HISTORICAL_INTERNAL;
    }
}
