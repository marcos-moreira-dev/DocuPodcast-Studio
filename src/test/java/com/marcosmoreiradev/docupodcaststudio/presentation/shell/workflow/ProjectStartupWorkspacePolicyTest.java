package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ProjectStartupWorkspacePolicyTest {
    @Test
    void everyProjectModeStartsInDocumentView() {
        for (ProjectMode mode : ProjectMode.officialModes()) {
            assertEquals(WorkspaceKind.DOCUMENT_READER,
                    ProjectStartupWorkspacePolicy.initialWorkspace(mode), mode.name());
        }
    }
}
