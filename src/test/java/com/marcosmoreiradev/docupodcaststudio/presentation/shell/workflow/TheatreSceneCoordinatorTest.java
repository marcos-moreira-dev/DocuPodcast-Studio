package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreSceneCoordinatorTest {
    @Test
    void addsUpdatesAndDeletesScenesWithDescriptions() {
        TheatreSceneCoordinator coordinator = new TheatreSceneCoordinator();
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Obra"));
        TheatreProjectLayer.TheatreAct act = coordinator.acts(Optional.of(session)).get(0);

        TheatreSceneCoordinator.SaveResult added = coordinator.addScene(
                Optional.of(session),
                act.id(),
                "Escena 1",
                "Hangar al amanecer.");
        String sceneId = session.project().theatre().scenes().get(0).id();

        TheatreSceneCoordinator.SaveResult secondAct = coordinator.addAct(
                Optional.of(session),
                "Acto 2",
                "Desenlace.");
        String secondActId = session.project().theatre().acts().get(1).id();
        TheatreSceneCoordinator.SaveResult renamedAct = coordinator.updateAct(
                Optional.of(session),
                secondActId,
                "Acto final",
                "Resolucion.");
        TheatreSceneCoordinator.SaveResult updated = coordinator.updateScene(
                Optional.of(session),
                sceneId,
                "Escena 1: El hangar",
                "Hangar con luz naranja.");

        assertTrue(added.saved());
        assertTrue(secondAct.saved());
        assertTrue(renamedAct.saved());
        assertTrue(updated.saved());
        assertEquals(act.id(), session.project().theatre().scenes().get(0).actId());
        assertEquals(2, session.project().theatre().acts().size());
        assertEquals("Acto final", session.project().theatre().acts().get(1).displayName());
        assertEquals("Resolucion.", session.project().theatre().acts().get(1).notes());
        assertEquals("Escena 1: El hangar", session.project().theatre().scenes().get(0).displayName());
        assertEquals("Hangar con luz naranja.", session.project().theatre().scenes().get(0).notes());

        TheatreProjectLayer theatreWithDependents = new TheatreProjectLayer(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                session.project().theatre().acts(),
                session.project().theatre().scenes(),
                List.of(new TheatreProjectLayer.SpatialPosition(sceneId, "T1", "", 0.2, 0.3, "")),
                List.of(new TheatreProjectLayer.TheatreAction(sceneId, "T1", "T2", "", "Cruza", true)),
                List.of(new TheatreProjectLayer.ObjectImage("OBJIMG-001", "OBJ-001", sceneId, "escena", "IMG-OBJ-001", "")),
                List.of());
        session.replaceProject(session.project().withTheatre(theatreWithDependents), true);

        TheatreSceneCoordinator.SaveResult deleted = coordinator.deleteScene(Optional.of(session), sceneId);

        assertTrue(deleted.saved());
        assertEquals(0, session.project().theatre().scenes().size());
        assertEquals(0, session.project().theatre().positions().size());
        assertEquals(0, session.project().theatre().actions().size());
        assertEquals(0, session.project().theatre().objectImages().size());
    }
}
