package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreImageGenerationWorkspaceViewTest {
    @Test
    void formatsElapsedGenerationTimeWithHours() {
        assertEquals("00:00:00", TheatreImageGenerationWorkspaceView.formatElapsed(0));
        assertEquals("01:02:03", TheatreImageGenerationWorkspaceView.formatElapsed(3_723));
        assertEquals("06:00:00", TheatreImageGenerationWorkspaceView.formatElapsed(21_600));
    }

    @Test
    void refinementRequiresUpscaleAndARealHigherTarget() {
        assertFalse(TheatreImageGenerationWorkspaceView.refinementControlsEnabled(false, true));
        assertFalse(TheatreImageGenerationWorkspaceView.refinementControlsEnabled(true, false));
        assertTrue(TheatreImageGenerationWorkspaceView.refinementControlsEnabled(true, true));
    }
}
