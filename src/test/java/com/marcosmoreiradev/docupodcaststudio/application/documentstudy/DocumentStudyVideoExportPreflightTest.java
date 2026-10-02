package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.NarratedFrameBinding;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentRectangle;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentStudyVideoExportPreflightTest {
    @TempDir Path temp;

    @Test
    void reportsMissingSpokenAudioBeforeRendererStarts() throws Exception {
        Files.writeString(temp.resolve("frame.png"), "image");
        var frame = new SimpleVideoFrame("F1", "SEG1", "Title", "spoken",
                "", "frame.png", "", 0, 0.2, true, false);
        var report = new DocumentStudyVideoExportPreflight().inspect(
                new SimpleVideoPlan("demo", List.of(frame), 0, Instant.now()), temp);
        assertFalse(report.ready());
        assertEquals(0, report.framesMissingImage());
        assertEquals(1, report.spokenFramesMissingAudio());
        assertEquals("SEG1", report.missingFrames().getFirst().narrationSegmentId());
    }

    @Test
    void acceptsOnlyPhysicallyCompleteTimeline() throws Exception {
        Files.writeString(temp.resolve("frame.png"), "image");
        writeWav(temp.resolve("audio.wav"), 1.0);
        var binding = binding(NarratedFrameBinding.Quality.CORRECT);
        var frame = new SimpleVideoFrame("F1", "SEG1", "Title", "spoken",
                "", "frame.png", "audio.wav", 1, 0.2, true, true,
                false, List.of(), List.of(), binding);
        assertTrue(new DocumentStudyVideoExportPreflight().inspect(
                new SimpleVideoPlan("demo", List.of(frame), 0, Instant.now()), temp).ready());
    }

    @Test
    void rejectsWrongNarrationVisualBindingBeforeRendererStarts() throws Exception {
        Files.writeString(temp.resolve("frame.png"), "image");
        writeWav(temp.resolve("audio.wav"), 1.0);
        var frame = new SimpleVideoFrame("F1", "SEG1", "Title", "spoken",
                "", "frame.png", "audio.wav", 1, 0.2, true, true,
                false, List.of(), List.of(), binding(NarratedFrameBinding.Quality.WRONG_VISUAL));
        var report = new DocumentStudyVideoExportPreflight().inspect(
                new SimpleVideoPlan("demo", List.of(frame), 0, Instant.now()), temp);
        assertFalse(report.ready());
        assertEquals("WRONG_VISUAL: test", report.invalidVisualBindings().getFirst().reason());
    }

    @Test
    void rejectsCorrectRegionWhenTheRenderedVisualDoesNotCoverTheNarratedFragment() throws Exception {
        Files.writeString(temp.resolve("frame.png"), "image");
        writeWav(temp.resolve("audio.wav"), 1.0);
        var covered = binding(NarratedFrameBinding.Quality.CORRECT);
        var wrongFragment = new NarratedFrameBinding(covered.frameId(), covered.segmentId(),
                covered.sourceBlockId(), covered.sourceBlockIds(), covered.regionId(),
                covered.sourceBlockType(), covered.pageNumber(), covered.readingOrder(),
                covered.presentationMode(), covered.visualSource(), covered.sourceBBox(),
                covered.cropBBox(), covered.imageRelativePath(), covered.audioPath(),
                covered.durationMillis(), covered.renderedTextHash(), covered.expectedSourceTextHash(),
                20, 40, "otro fragmento", List.of(1), List.of(),
                covered.expectedFragmentBboxes(), false,
                NarratedFrameBinding.Quality.CORRECT, "region correct but fragment wrong");
        var frame = new SimpleVideoFrame("F1", "SEG1", "Title", "spoken",
                "", "frame.png", "audio.wav", 1, 0.2, true, true,
                false, List.of(), List.of(), wrongFragment);

        var report = new DocumentStudyVideoExportPreflight().inspect(
                new SimpleVideoPlan("demo", List.of(frame), 0, Instant.now()), temp);

        assertFalse(report.ready());
        assertTrue(report.invalidVisualBindings().getFirst().reason()
                .startsWith("WRONG_FRAGMENT_VISUAL"));
    }

    @Test
    void comparesBindingAgainstExplicitPrimarySourceInNarrationScript() throws Exception {
        Files.writeString(temp.resolve("frame.png"), "image");
        writeWav(temp.resolve("audio.wav"), 1.0);
        var frame = new SimpleVideoFrame("F1", "SEG1", "Title", "spoken",
                "", "frame.png", "audio.wav", 1, 0.2, true, true,
                false, List.of(), List.of(), binding(NarratedFrameBinding.Quality.CORRECT));
        NarrationSegment segment = new NarrationSegment("SEG1", NarrationSegmentType.PARAGRAPH,
                "Title", "spoken", List.of("PDF-R1"), "CHR", "VOC", "STY",
                Map.of("sourceBlockId", "PDF-R2"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "demo", "es", "source.pdf", List.of(segment));

        var report = new DocumentStudyVideoExportPreflight().inspect(
                new SimpleVideoPlan("demo", List.of(frame), 0, Instant.now()), temp, script);

        assertFalse(report.ready());
        assertEquals("PRIMARY_SOURCE_BLOCK_ID_MISMATCH",
                report.invalidVisualBindings().getFirst().reason());
    }

    @Test
    void rejectsPlannedDurationThatDoesNotMatchPhysicalWav() throws Exception {
        Files.writeString(temp.resolve("frame.png"), "image");
        writeWav(temp.resolve("audio.wav"), 1.0);
        var frame = new SimpleVideoFrame("F1", "SEG1", "Title", "spoken",
                "", "frame.png", "audio.wav", 4.0, 0.2, true, true);
        var report = new DocumentStudyVideoExportPreflight().inspect(
                new SimpleVideoPlan("demo", List.of(frame), 0, Instant.now()), temp);
        assertFalse(report.ready());
        assertEquals(1, report.durationMismatches().size());
    }

    private static void writeWav(Path target, double seconds) throws Exception {
        float sampleRate = 8_000f;
        byte[] pcm = new byte[(int) (sampleRate * seconds) * 2];
        var format = new javax.sound.sampled.AudioFormat(sampleRate, 16, 1, true, false);
        try (var input = new java.io.ByteArrayInputStream(pcm);
             var stream = new javax.sound.sampled.AudioInputStream(
                     input, format, pcm.length / format.getFrameSize())) {
            javax.sound.sampled.AudioSystem.write(stream,
                    javax.sound.sampled.AudioFileFormat.Type.WAVE, target.toFile());
        }
    }

    private static NarratedFrameBinding binding(NarratedFrameBinding.Quality quality) {
        var box = new DocumentContentRectangle(10, 10, 20, 20);
        return new NarratedFrameBinding("F1", "SEG1", "PDF-R1", List.of("PDF-R1"), "PDF-R1",
                "PARAGRAPH", 1, 0, DocumentPresentationMode.SOURCE_CAPTURE,
                NarratedFrameBinding.VisualSource.PDF_REGION_CROP, box, box,
                "frame.png", "audio.wav", 1000, "", "",
                0, 10, "fragment", List.of(0), List.of(), List.of(box), true,
                quality, "test");
    }
}
