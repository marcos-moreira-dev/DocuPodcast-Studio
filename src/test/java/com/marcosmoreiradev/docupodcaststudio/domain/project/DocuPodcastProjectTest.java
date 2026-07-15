package com.marcosmoreiradev.docupodcaststudio.domain.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocuPodcastProjectTest {
    @Test
    void newProjectStartsEmptyWithWelcomeWorkspace() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Mis notas");

        assertEquals(ProjectKind.EMPTY, project.metadata().kind());
        assertEquals("WELCOME_HOME", project.viewState().get("activeWorkspace"));
        assertEquals(0, project.assets().size());
    }

    @Test
    void withAssetReturnsNewProject() {
        DocuPodcastProject project = DocuPodcastProject.createNew("Mis notas");
        ProjectAssetReference asset = new ProjectAssetReference("SRC-001", ProjectAssetKind.SOURCE_DOCUMENT,
                "Notas", "source/notas.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Fuente", "", "");

        DocuPodcastProject updated = project.withAsset(asset);

        assertEquals(0, project.assets().size());
        assertEquals(1, updated.assets().size());
        assertTrue(updated.assets().byId("SRC-001").isPresent());
    }
}
