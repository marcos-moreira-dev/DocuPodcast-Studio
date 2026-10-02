package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.NarratedFrameBinding;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentRectangle;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildDiagnosticDocumentStudyVideoPlanUseCaseTest {
    @TempDir Path root;

    @Test
    void reusesNormalAudioAndTimelineAndOnlyDecoratesVisuals() throws Exception {
        ImageIO.write(new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB),
                "png", root.resolve("normal.png").toFile());
        Files.write(root.resolve("audio.wav"), new byte[]{1});
        var binding = new NarratedFrameBinding("FRAME-1", "SEG-1", "BLOCK-1",
                List.of("BLOCK-1"), "REGION-1", "PARAGRAPH", 3, 7,
                DocumentPresentationMode.SOURCE_CAPTURE,
                NarratedFrameBinding.VisualSource.PDF_REGION_CROP,
                new DocumentContentRectangle(10, 20, 100, 120),
                new DocumentContentRectangle(5, 15, 105, 125),
                "normal.png", "audio.wav", 2000, "", "",
                0, 10, "fragment", List.of(0), List.of(),
                List.of(new DocumentContentRectangle(10, 20, 100, 120)), true,
                NarratedFrameBinding.Quality.CORRECT, "exact-source-binding");
        var frame = new SimpleVideoFrame("FRAME-1", "SEG-1", "Title", "speech",
                "", "normal.png", "audio.wav", 2.0, 0.35, true, true,
                false, List.of(), List.of(), binding);
        var normal = new SimpleVideoPlan("Normal", List.of(frame), 0.35, Instant.EPOCH);

        var diagnostic = new BuildDiagnosticDocumentStudyVideoPlanUseCase().build(normal, root);

        assertEquals(normal.frameCount(), diagnostic.frameCount());
        assertEquals(normal.totalDurationSeconds(), diagnostic.totalDurationSeconds());
        assertEquals(normal.frames().getFirst().audioRelativePath(),
                diagnostic.frames().getFirst().audioRelativePath());
        assertTrue(Files.isRegularFile(root.resolve(
                diagnostic.frames().getFirst().imageRelativePath())));
        assertEquals(binding.sourceBlockId(),
                diagnostic.frames().getFirst().visualBinding().sourceBlockId());
        assertEquals(binding.presentationMode(),
                diagnostic.frames().getFirst().visualBinding().presentationMode());
        assertEquals(binding.visualSource(),
                diagnostic.frames().getFirst().visualBinding().visualSource());
        Path manifest = root.resolve("exports/frame-binding-manifest-diagnostic.tsv");
        assertTrue(Files.isRegularFile(manifest));
        String manifestText = Files.readString(manifest);
        assertTrue(manifestText.contains("BLOCK-1"));
        assertTrue(manifestText.contains("PDF_REGION_CROP"));
    }
}
