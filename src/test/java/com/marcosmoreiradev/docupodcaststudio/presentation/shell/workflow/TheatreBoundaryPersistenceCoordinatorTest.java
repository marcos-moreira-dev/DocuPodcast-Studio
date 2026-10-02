package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionBoundaryStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreBoundaryPersistenceCoordinatorTest {
    @Test
    void hydrateCompletesMissingScenesAndRepairsCrossSceneBoundaries() {
        TheatreProjectLayer.Scene first = new TheatreProjectLayer.Scene("SC-1", "Cuadro I", "", "ACT-1");
        TheatreProjectLayer.Scene second = new TheatreProjectLayer.Scene("SC-2", "Cuadro II", "", "ACT-1");
        TheatreProjectLayer layer = new TheatreProjectLayer(
                List.of(
                        new TheatreProjectLayer.Intervencion("INTERVENCION-10001", "B-INTERVENCION-10001", 1),
                        new TheatreProjectLayer.Intervencion("INTERVENCION-1", "B-INTERVENCION-1", 2),
                        new TheatreProjectLayer.Intervencion("INTERVENCION-10002", "B-INTERVENCION-10002", 3),
                        new TheatreProjectLayer.Intervencion("INTERVENCION-2", "B-INTERVENCION-2", 4)),
                List.of(), List.of(), List.of(), List.of(), List.of(first, second), List.of(), List.of())
                .withTextActionPlacements(List.of(
                        placement("INTERVENCION-10001", first.id()),
                        placement("INTERVENCION-1", first.id()),
                        placement("INTERVENCION-10002", second.id()),
                        placement("INTERVENCION-2", second.id())));
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION)
                .withTheatre(layer)
                .withViewState("theatre.sceneBoundary.SC-1", "INTERVENCION-10001|INTERVENCION-10002");
        IntervencionBoundaryStore store = new IntervencionBoundaryStore();

        new TheatreBoundaryPersistenceCoordinator().hydrate(project, store);

        assertEquals(new IntervencionBoundaryStore.SceneBoundary(
                "INTERVENCION-10001", "INTERVENCION-1"), store.limite("SC-1"));
        assertEquals(new IntervencionBoundaryStore.SceneBoundary(
                "INTERVENCION-10002", "INTERVENCION-2"), store.limite("SC-2"));
    }

    private static TheatreProjectLayer.TextActionPlacement placement(String interventionId, String sceneId) {
        return new TheatreProjectLayer.TextActionPlacement(
                interventionId, sceneId, "CHR-ACOTACION", "", "", "", Map.of());
    }
}
