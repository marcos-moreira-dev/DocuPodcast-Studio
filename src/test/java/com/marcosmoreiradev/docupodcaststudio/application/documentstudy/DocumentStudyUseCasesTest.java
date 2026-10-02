package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;

import com.marcosmoreiradev.docupodcaststudio.application.fragment.BuildFragmentWorkspaceProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.reading.PreparedReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildNarrationScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyUseCasesTest {
    @TempDir
    Path tempDir;

    @Test
    void projectionKeepsVisualSupportAndTreatsWordMathAsOrdinaryNarration() throws Exception {
        ReadableDocument document = documentWithSupport();
        NarrationScriptDocument script = new BuildNarrationScriptUseCase()
                .build(document, "es", false, TableNarrationPolicy.READ_STRUCTURED);
        Files.createDirectories(tempDir.resolve("jobs/JOB-001"));
        Files.write(tempDir.resolve("jobs/JOB-001/SEG-001.wav"), new byte[]{1, 2, 3});
        AudioJobSnapshot job = audioJob("SEG-001", "jobs/JOB-001/SEG-001.wav");
        var fragments = new BuildFragmentWorkspaceProjectionUseCase()
                .build(document, script, null, null, List.of(job), PlaybackManifest.empty());

        DocumentStudyProjection projection = new BuildDocumentStudyProjectionUseCase()
                .build(document, PreparedReadingProjection.from(document, script), fragments, List.of(job), tempDir);

        assertEquals(2, projection.primaryFragmentCount());
        assertEquals(2, projection.narratableFragmentCount());
        assertEquals(1, projection.secondaryUnitCount());
        assertEquals(3, projection.sourceVisualCount());
        assertEquals(3, projection.supportItems().size());
        assertEquals(1, projection.audioReadyCount());
        assertEquals(1, projection.audioMissingCount());
        assertFalse(projection.readiness().textAudioVideoExportable());
    }

    @Test
    void textAudioVideoPlanWritesTemporaryFramesAndDoesNotRequireProjectAssets() throws Exception {
        NarrationScriptDocument script = NarrationScriptDocument.create("Documento", "es", "documento.txt", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto completo del fragmento.", List.of("B001")),
                new NarrationSegment("SEG-002", NarrationSegmentType.TABLE_NOTICE, "Tabla", "Lectura de cuadro.",
                        List.of("B002"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                        Map.of("secondaryReadUnit", "true"))));
        Files.createDirectories(tempDir.resolve("jobs/JOB-001"));
        Files.write(tempDir.resolve("jobs/JOB-001/SEG-001.wav"), new byte[]{1, 2, 3});

        SimpleVideoPlan plan = new BuildDocumentStudyTextVideoPlanUseCase().build(
                script,
                List.of(audioJob("SEG-001", "jobs/JOB-001/SEG-001.wav")),
                tempDir,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.HD_720));

        assertEquals(1, plan.frameCount());
        assertTrue(plan.exportableAsRenderedVideo());
        assertTrue(plan.frames().getFirst().imageRelativePath().startsWith("exports/document-study-frames/"));
        assertTrue(plan.frames().getFirst().imageAssetId().isBlank());
    }

    @Test
    void textAudioVideoPlanAcceptsUnitAudioChunksForParentSegment() throws Exception {
        NarrationScriptDocument script = NarrationScriptDocument.create("Documento", "es", "documento.txt", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto completo del fragmento.", List.of("B001"))));
        Files.createDirectories(tempDir.resolve("jobs/JOB-001"));
        Files.write(tempDir.resolve("jobs/JOB-001/SEG-001-U001.wav"), new byte[]{1, 2, 3});
        Files.write(tempDir.resolve("jobs/JOB-001/SEG-001-U002.wav"), new byte[]{1, 2, 3});
        AudioSegmentSnapshot first = AudioSegmentSnapshot.pending("SEG-001-U001", "SEG-001-U001")
                .completed("jobs/JOB-001/SEG-001-U001.wav", 1.0);
        AudioSegmentSnapshot second = AudioSegmentSnapshot.pending("SEG-001-U002", "SEG-001-U002")
                .completed("jobs/JOB-001/SEG-001-U002.wav", 1.0);

        SimpleVideoPlan plan = new BuildDocumentStudyTextVideoPlanUseCase().build(
                script,
                List.of(audioJob(List.of(first, second))),
                tempDir,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.HD_720));

        assertEquals(2, plan.frameCount());
        assertEquals(2, plan.framesWithAudio());
        assertTrue(plan.exportableAsRenderedVideo());
        assertEquals("SEG-001-U001", plan.frames().get(0).segmentId());
        assertEquals("SEG-001-U002", plan.frames().get(1).segmentId());
    }

    private static ReadableDocument documentWithSupport() {
        return new ReadableDocument("Documento", SourceDocumentFormat.DOCX, Path.of("documento.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Texto narrable", "Normal"),
                DocumentBlock.of("B002", DocumentBlockType.TABLE_NOTICE, "Tabla fuente", "table",
                        Map.of("table.rowCount", "2", "table.columnCount", "1", "table.header.0", "H", "table.cell.1.0", "v")),
                DocumentBlock.of("B003", DocumentBlockType.IMAGE_NOTICE, "Imagen fuente", "image",
                        Map.of("embeddedImagePath", "word/media/image1.png")),
                DocumentBlock.of("B004", DocumentBlockType.MATH_NOTICE, "Formula fuente", "math",
                        Map.of("visualBlock", "true"))));
    }

    private static AudioJobSnapshot audioJob(String segmentId, String audioPath) {
        AudioSegmentSnapshot segment = AudioSegmentSnapshot.pending(segmentId, segmentId).completed(audioPath, 1.0);
        return audioJob(List.of(segment));
    }

    private static AudioJobSnapshot audioJob(List<AudioSegmentSnapshot> segments) {
        String firstSegmentId = segments.isEmpty() ? "" : segments.getFirst().segmentId();
        return new AudioJobSnapshot(
                "JOB-001",
                "Documento",
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                segments.size(),
                segments.size(),
                0,
                1.0,
                firstSegmentId,
                firstSegmentId,
                0,
                "OK",
                "jobs/JOB-001",
                "",
                "jobs/JOB-001/manifest.json",
                segments,
                Instant.EPOCH,
                Instant.EPOCH);
    }
}
