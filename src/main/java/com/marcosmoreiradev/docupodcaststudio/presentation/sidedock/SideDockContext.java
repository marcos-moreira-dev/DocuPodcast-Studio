package com.marcosmoreiradev.docupodcaststudio.presentation.sidedock;

import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;

/** Context used by side-dock modules to decide if they apply to a workspace. */
public record SideDockContext(WorkspaceKind workspaceKind, String title) {
    public boolean documentLike() {
        return workspaceKind == WorkspaceKind.DOCUMENT_READER;
    }
}
