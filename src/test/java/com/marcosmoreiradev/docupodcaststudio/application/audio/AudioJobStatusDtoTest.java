package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioJobStatusDtoTest {
    @Test
    void calculatesProgressFromSegmentsAndFormatsEta() {
        AudioJobStatusDto status = new AudioJobStatusDto("JOB-001", "Guion", AudioJobState.GENERATING_AUDIO,
                AudioGenerationStage.GENERATING_SEGMENTS, 5, 10, 0, 0.0,
                "SEG-005", "Objetivos", 125, "Generando", "jobs/JOB-001", "", "");

        assertEquals(0.5, status.progress(), 0.001);
        assertEquals("50%", status.progressPercentLabel());
        assertTrue(status.etaLabel().contains("2 min 5 s"));
        assertTrue(status.statusLine().contains("5/10 segmentos"));
    }

    @Test
    void idleStatusIsSafeForUi() {
        AudioJobStatusDto idle = AudioJobStatusDto.idle();

        assertEquals(AudioJobState.IDLE, idle.state());
        assertEquals("0/0 segmentos", idle.segmentCounterLabel());
        assertEquals("Sin ETA", idle.etaLabel());
    }
}
