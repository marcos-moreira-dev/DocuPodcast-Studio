package com.marcosmoreiradev.docupodcaststudio.presentation.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AudioJobRowTest {
    @Test
    void activeStatusIsProjectedAsQueueRow() {
        AudioJobStatusDto status = new AudioJobStatusDto(
                "JOB-22", "Proyecto", AudioJobState.GENERATING_AUDIO, AudioGenerationStage.GENERATING_SEGMENTS,
                2, 4, 0, 0.5, "SEG-003", "Tercer segmento", 75,
                "Generando", "C:/temp/jobs/JOB-22", "", "jobs/JOB-22/audio-manifest.json");

        AudioJobRow row = AudioJobRow.active(status);

        assertTrue(row.active());
        assertTrue(row.running());
        assertEquals("50%", row.progressLabel());
        assertTrue(row.headline().contains("JOB-22"));
        assertTrue(row.supportLine().contains("SEG-003"));
    }
}
