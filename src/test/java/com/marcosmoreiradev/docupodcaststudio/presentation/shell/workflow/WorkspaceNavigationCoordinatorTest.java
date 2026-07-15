package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSessionCoordinator;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceNavigationCoordinatorTest {
    @Test
    void rememberingWorkspaceDoesNotDirtyACleanSession() {
        ProjectSessionCoordinator sessions = new ProjectSessionCoordinator();
        ProjectSession session = sessions.open(DocuPodcastProject.createNew("Demo"), java.nio.file.Path.of("demo.docupodcast.json"));
        WorkspaceNavigationCoordinator coordinator = new WorkspaceNavigationCoordinator(sessions);

        WorkspaceKind selected = coordinator.rememberWorkspace(WorkspaceKind.AUDIO_JOBS);

        assertEquals(WorkspaceKind.DOCUMENT_READER, selected,
                "las superficies legacy no deben volver como workspaces visibles");
        assertEquals("DOCUMENT_READER", session.project().viewState().get(WorkspaceNavigationCoordinator.ACTIVE_WORKSPACE_KEY));
        assertFalse(session.dirty(), "navegar no debe marcar contenido como editado");
    }

    @Test
    void rememberingWorkspacePreservesExistingDirtyState() {
        ProjectSessionCoordinator sessions = new ProjectSessionCoordinator();
        ProjectSession session = sessions.startNew(DocuPodcastProject.createNew("Demo"));
        assertTrue(session.dirty());

        WorkspaceNavigationCoordinator coordinator = new WorkspaceNavigationCoordinator(sessions);
        WorkspaceKind selected = coordinator.rememberWorkspace(WorkspaceKind.STORYBOARD);

        assertEquals(WorkspaceKind.DOCUMENT_READER, selected,
                "Storyboard vive como rail del Documento, no como workspace restaurable");
        assertEquals("DOCUMENT_READER", session.project().viewState().get(WorkspaceNavigationCoordinator.ACTIVE_WORKSPACE_KEY));
        assertTrue(session.dirty(), "navegar no debe limpiar cambios pendientes");
    }
}
