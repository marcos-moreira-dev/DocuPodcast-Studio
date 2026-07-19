package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreImageGenerationWorkspaceViewTest {
    @Test
    void formatsElapsedGenerationTimeWithHours() {
        assertEquals("00:00:00", TheatreImageGenerationWorkspaceView.formatElapsed(0));
        assertEquals("01:02:03", TheatreImageGenerationWorkspaceView.formatElapsed(3_723));
        assertEquals("06:00:00", TheatreImageGenerationWorkspaceView.formatElapsed(21_600));
    }
}
