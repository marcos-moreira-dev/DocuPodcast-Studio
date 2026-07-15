package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSessionCoordinatorTest {
    @Test
    void newProjectStartsDirtyAndUnsaved() {
        ProjectSessionCoordinator coordinator = new ProjectSessionCoordinator();
        coordinator.startNew(DocuPodcastProject.createNew("Prueba"));

        assertTrue(coordinator.hasActiveSession());
        assertTrue(coordinator.dirty());
        assertFalse(coordinator.saveable());
    }

    @Test
    void openedProjectStartsCleanAndSaveable() {
        ProjectSessionCoordinator coordinator = new ProjectSessionCoordinator();
        coordinator.open(DocuPodcastProject.createNew("Prueba"), Path.of("Prueba.docupodcast.json"));

        assertTrue(coordinator.hasActiveSession());
        assertFalse(coordinator.dirty());
        assertTrue(coordinator.saveable());
    }
}
