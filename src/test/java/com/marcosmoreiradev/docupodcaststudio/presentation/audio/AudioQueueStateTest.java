package com.marcosmoreiradev.docupodcaststudio.presentation.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class AudioQueueStateTest {
    @Test
    void projectsPersistedSnapshotsAsOperationalQueueRows() {
        AudioSegmentSnapshot completed = AudioSegmentSnapshot.pending("SEG-001", "Intro").completed("jobs/JOB-1/audio/SEG-001.wav", 2.5);
        AudioSegmentSnapshot pending = AudioSegmentSnapshot.pending("SEG-002", "Cuerpo");
        AudioJobSnapshot snapshot = new AudioJobSnapshot(
                "JOB-1", "Demo", AudioJobState.FAILED, AudioGenerationStage.FAILED,
                1, 2, 1, 0.5, "SEG-002", "Cuerpo", 60,
                "Falló el segundo segmento", "jobs/JOB-1", "", "jobs/JOB-1/audio-manifest.json",
                List.of(completed, pending), Instant.now(), Instant.now());

        AudioQueueState state = AudioQueueState.saved(
                "Mock seguro", AudioJobStatusDto.idle(), Path.of("/tmp/proyecto"),
                List.of(snapshot), List.of("detalle"), List.of("diagnóstico"), List.of("cue"));

        assertTrue(state.projectSaved());
        assertEquals(1, state.persistedJobs().size());
        assertTrue(state.hasRecoverableJob());
        assertTrue(state.queueLabels().get(0).contains("JOB-1"));
        assertTrue(state.selectedJobDetails().contains("detalle"));
        assertTrue(state.processDiagnostics().contains("diagnóstico"));
        assertTrue(state.playbackCues().contains("cue"));
    }

    @Test
    void unsavedProjectExplainsThatQueueRequiresProjectFolder() {
        AudioQueueState state = AudioQueueState.unsaved("Motor", AudioJobStatusDto.idle(), List.of());

        assertFalse(state.projectSaved());
        assertEquals(1, state.queueLabels().size());
        assertTrue(state.queueLabels().get(0).contains("Guarda el proyecto"));
    }
}
