package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionSelectionRef;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StudioSessionStoreTest {
    @Test void containsOnlyCrossExperienceSessionState() {
        StudioSessionStore store = new StudioSessionStore();
        DocuPodcastProject project = DocuPodcastProject.empty("Proyecto");
        store.updateProject(project, true, true);
        store.activeWorkspaceProperty().set(WorkspaceKind.DOCUMENT_READER);
        store.selectedPdfRegionState().set(new PdfRegionSelectionRef(12, "region-12-a", 3, 18));
        store.updateStatus("  Guardado pendiente  ");

        StudioSessionStore.Snapshot snapshot = store.snapshot();
        assertSame(project, snapshot.activeProject());
        assertEquals(WorkspaceKind.DOCUMENT_READER, snapshot.activeWorkspace());
        assertTrue(snapshot.projectOpen());
        assertTrue(snapshot.dirty());
        assertEquals("Guardado pendiente", snapshot.statusMessage());
        assertEquals(12, snapshot.selectedPdfRegion().pageNumber());
        assertEquals("region-12-a", snapshot.selectedPdfRegion().regionId());

        store.clearProject();
        assertNull(store.activeProjectProperty().get());
        assertFalse(store.projectOpenProperty().get());
    }
}
