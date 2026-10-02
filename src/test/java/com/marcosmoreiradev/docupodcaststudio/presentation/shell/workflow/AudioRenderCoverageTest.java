package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioRenderCoverageTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsRenderUnitAudioAsCoverageForItsSourceSegment() throws Exception {
        createAudio("jobs/JOB-UNIT/audio/SEG-005-U001.wav");

        assertTrue(AudioRenderCoverage.hasAllChunksRendered(
                script(),
                List.of(job("SEG-005-U001", "jobs/JOB-UNIT/audio/SEG-005-U001.wav")),
                tempDir));
    }

    @Test
    void rejectsCompletedMetadataWhenAudioFileIsMissing() {
        assertFalse(AudioRenderCoverage.hasAllChunksRendered(
                script(),
                List.of(job("SEG-005-U001", "jobs/JOB-UNIT/audio/SEG-005-U001.wav")),
                tempDir));
    }

    private NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-005", NarrationSegmentType.PARAGRAPH,
                        "Texto 5", "CAPITAN BIGOTE: Revise el combustible.", List.of("B0005"))));
    }

    private AudioJobSnapshot job(String segmentId, String audioPath) {
        return new AudioJobSnapshot("JOB-UNIT", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-UNIT", "", "jobs/JOB-UNIT/audio-manifest.json", List.of(
                new AudioSegmentSnapshot(segmentId, "Texto 5", AudioSegmentStatus.COMPLETED,
                        audioPath, 2.0, 1, "",
                        AudioGenerationUnit.fromSegment(script().segments().getFirst()).sourceFingerprint())
        ), Instant.now(), Instant.now());
    }

    @Test
    void rejectsExistingAudioWithObsoleteSourceFingerprint() throws Exception {
        createAudio("jobs/JOB-UNIT/audio/SEG-005-U001.wav");
        var stale = new AudioSegmentSnapshot("SEG-005-U001", "Texto 5", AudioSegmentStatus.COMPLETED,
                "jobs/JOB-UNIT/audio/SEG-005-U001.wav", 2.0, 1, "");
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-UNIT", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-UNIT", "", "jobs/JOB-UNIT/audio-manifest.json", List.of(stale),
                Instant.now(), Instant.now());

        assertFalse(AudioRenderCoverage.hasAllChunksRendered(script(), List.of(job), tempDir));
    }

    private void createAudio(String relativePath) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[64]);
    }
}
