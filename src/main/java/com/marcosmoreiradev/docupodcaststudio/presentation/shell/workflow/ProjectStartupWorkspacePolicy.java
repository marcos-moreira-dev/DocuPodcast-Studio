package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;

/** Defines the predictable workspace shown immediately after opening a project. */
final class ProjectStartupWorkspacePolicy {
    private ProjectStartupWorkspacePolicy() {
    }

    static WorkspaceKind initialWorkspace(ProjectMode mode) {
        return WorkspaceKind.DOCUMENT_READER;
    }
}
