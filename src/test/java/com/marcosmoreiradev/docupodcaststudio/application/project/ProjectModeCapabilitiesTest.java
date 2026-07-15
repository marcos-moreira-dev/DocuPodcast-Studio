package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectModeCapabilitiesTest {
    @Test
    void documentaryKeepsDocumentAndAudioWithoutNarrativeVisuals() {
        ProjectModeCapabilities capabilities = ProjectModeCapabilities.forMode(ProjectMode.DOCUMENTARY_STUDIO);

        assertTrue(capabilities.documentStudy());
        assertTrue(capabilities.audioProduction());
        assertFalse(capabilities.narrativeVisuals());
        assertFalse(capabilities.theatreProduction());
    }

    @Test
    void narrativeVideoAddsVisualsAndTheatreAddsTheatreLayer() {
        ProjectModeCapabilities narrative = ProjectModeCapabilities.forMode(ProjectMode.NARRATIVE_VIDEO);
        ProjectModeCapabilities theatre = ProjectModeCapabilities.forMode(ProjectMode.THEATRE_PRODUCTION);

        assertTrue(narrative.narrativeVisuals());
        assertFalse(narrative.theatreProduction());
        assertTrue(theatre.narrativeVisuals());
        assertTrue(theatre.theatreProduction());
    }
}
