package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSessionCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceSurfacePolicy;

import java.util.Objects;

/**
 * Coordinates shell navigation without treating every workspace change as edited product content.
 *
 * <p>This coordinator does not mark the project as dirty when the user only changes
 * workspace. It preserves the existing dirty flag while remembering the last product surface.</p>
 *
 * <p>Legacy implementation surfaces such as internal preparation, audio jobs and visual sequencing are normalized
 * to Documento so they do not come back as visible workspaces after reopening a project.</p>
 */
public final class WorkspaceNavigationCoordinator {
    public static final String ACTIVE_WORKSPACE_KEY = "activeWorkspace";

    private final ProjectSessionCoordinator sessions;
    private final WorkspaceSurfacePolicy surfacePolicy = new WorkspaceSurfacePolicy();

    public WorkspaceNavigationCoordinator(ProjectSessionCoordinator sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    public WorkspaceKind rememberWorkspace(WorkspaceKind requestedWorkspace) {
        WorkspaceKind workspace = surfacePolicy.restoreStartupWorkspace(requestedWorkspace);
        sessions.activeSession().ifPresent(session -> session.replaceProjectPreservingDirty(
                session.project().withViewState(ACTIVE_WORKSPACE_KEY, workspace.name())));
        return workspace;
    }
}
