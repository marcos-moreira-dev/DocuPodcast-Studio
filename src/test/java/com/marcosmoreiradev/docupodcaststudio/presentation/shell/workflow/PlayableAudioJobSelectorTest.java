package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PlayableAudioJobSelectorTest {
    @Test
    void treatsRenderUnitAudioAsCompatibleWithItsSourceSegment() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Hola.", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Cierre", "Adios.", List.of("B002"))
        ));
        AudioJobSnapshot staleSegmentJob = snapshot("JOB-OLD", 5, List.of(
                AudioSegmentSnapshot.pending("SEG-001", "Intro").completed("jobs/JOB-OLD/audio/SEG-001.wav", 1.0)
        ));
        AudioJobSnapshot renderUnitJob = snapshot("JOB-UNITS", 10, List.of(
                AudioSegmentSnapshot.pending("SEG-001-U001", "Intro U1").completed("jobs/JOB-UNITS/audio/SEG-001-U001.wav", 1.0),
                AudioSegmentSnapshot.pending("SEG-002-U001", "Cierre U1").completed("jobs/JOB-UNITS/audio/SEG-002-U001.wav", 1.0)
        ));

        AudioJobSnapshot selected = new PlayableAudioJobSelector()
                .select(List.of(staleSegmentJob, renderUnitJob), "", script)
                .orElseThrow();

        assertEquals("JOB-UNITS", selected.jobId());
    }

    private static AudioJobSnapshot snapshot(String id, long updatedSecond, List<AudioSegmentSnapshot> segments) {
        return new AudioJobSnapshot(
                id,
                "Obra",
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                segments.size(),
                segments.size(),
                0,
                1.0,
                "",
                "",
                0,
                "OK",
                "jobs/" + id,
                "",
                "jobs/" + id + "/audio-manifest.json",
                segments,
                Instant.EPOCH,
                Instant.ofEpochSecond(updatedSecond));
    }
}
