package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobStage;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioJobProcessMapperTest {
    @Test
    void mapsAudioJobIntoCommonProcessSnapshot() {
        AudioJobSnapshot audioJob = new AudioJobSnapshot(
                "JOB-AUDIO-001",
                "Documento",
                AudioJobState.GENERATING_AUDIO,
                AudioGenerationStage.GENERATING_SEGMENTS,
                1,
                3,
                0,
                0.33,
                "SEG-002",
                "Segundo fragmento",
                24,
                "Generando voz",
                "jobs/JOB-AUDIO-001",
                "jobs/JOB-AUDIO-001/final/documento.wav",
                "jobs/JOB-AUDIO-001/audio-manifest.json",
                List.of(
                        new AudioSegmentSnapshot("SEG-001", "Uno", com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus.COMPLETED, "jobs/JOB-AUDIO-001/audio/SEG-001.wav", 1.2, 1, ""),
                        AudioSegmentSnapshot.pending("SEG-002", "Dos"),
                        AudioSegmentSnapshot.pending("SEG-003", "Tres")
                ),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:05Z")
        );

        var process = new AudioJobProcessMapper().map(audioJob);

        assertEquals(ProcessJobKind.TTS_AUDIO, process.kind());
        assertEquals(ProcessJobState.RUNNING, process.state());
        assertEquals(ProcessJobStage.RUNNING_ENGINE, process.stage());
        assertEquals("SEG-002", process.currentItemId());
        assertTrue(process.cancellable());
        assertTrue(process.recoverable());
        assertEquals(2, process.artifacts().size());
        assertTrue(process.logs().hasAnyLog());
    }
}
