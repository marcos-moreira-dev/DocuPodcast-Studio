package com.marcosmoreiradev.docupodcaststudio.domain.project;

import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectModePolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ProjectModeTest {
    @Test
    void exposesOfficialSpanishLabelsAndDocumentaryDefault() {
        assertEquals("Estudio documental", ProjectMode.DOCUMENTARY_STUDIO.displayName());
        assertEquals("Video narrativo", ProjectMode.NARRATIVE_VIDEO.displayName());
        assertEquals("Producción teatral", ProjectMode.THEATRE_PRODUCTION.displayName());
        assertEquals(ProjectMode.DOCUMENTARY_STUDIO, ProjectMode.defaultMode());
        assertEquals(ProjectMode.DOCUMENTARY_STUDIO, ProjectMetadata.create("Notas").mode());
    }

    @Test
    void infersLegacyModeFromKindAndTheatreLayer() {
        ProjectModePolicy policy = new ProjectModePolicy();
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "BLK-1")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());

        assertEquals(ProjectMode.DOCUMENTARY_STUDIO, policy.inferLegacy(ProjectKind.AUDIO_PROJECT, TheatreProjectLayer.empty()));
        assertEquals(ProjectMode.NARRATIVE_VIDEO, policy.inferLegacy(ProjectKind.FULL_PROJECT, TheatreProjectLayer.empty()));
        assertEquals(ProjectMode.THEATRE_PRODUCTION, policy.inferLegacy(ProjectKind.FULL_PROJECT, theatre));
    }

    @Test
    void theatreLayerWinsWhenResolvingVisibleModeForCompatibleOldProjects() {
        ProjectModePolicy policy = new ProjectModePolicy();
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "BLK-1")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra vieja", ProjectMode.DOCUMENTARY_STUDIO)
                .withTheatre(theatre);

        assertEquals(ProjectMode.THEATRE_PRODUCTION, policy.resolve(project));
    }
}
