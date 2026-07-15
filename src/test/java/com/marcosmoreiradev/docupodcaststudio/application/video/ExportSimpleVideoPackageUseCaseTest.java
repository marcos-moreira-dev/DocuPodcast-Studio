package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportSimpleVideoPackageUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsAuditableSimpleVideoPackageWithoutEmbeddingEncoder() throws Exception {
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto narrado", List.of("BLK-001"))
        ));
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-001", "Intro", AudioSegmentStatus.COMPLETED, "jobs/JOB-001/audio/SEG-001.wav", 2.0, 1, "")
        ), Instant.now(), Instant.now());

        SimpleVideoPackageExportResult result = new ExportSimpleVideoPackageUseCase()
                .export(DocuPodcastProject.createNew("Demo"), script, null, List.of(job), tempDir.resolve("video"));

        assertEquals(1, result.frameCount());
        assertTrue(Files.exists(result.planMarkdownFile()));
        assertTrue(Files.exists(result.framesCsvFile()));
        assertTrue(Files.exists(result.ffmpegConcatFile()));
        assertTrue(Files.exists(result.renderManifestFile()));
        assertTrue(Files.exists(result.renderCommandsFile()));
        assertTrue(Files.exists(result.renderStateFile()));
        assertTrue(Files.readString(result.planMarkdownFile()).contains("Plan de video simple"));
        assertTrue(Files.readString(result.planMarkdownFile()).contains("RENDER_MANIFEST.json"));
        assertTrue(Files.readString(result.framesCsvFile()).contains("SEG-001"));
        assertTrue(Files.readString(result.renderScriptFile()).contains("FFmpeg"));
        assertTrue(Files.readString(result.renderCommandsFile()).contains("docupodcast-simple-video-render-v1"));
        assertTrue(Files.readString(result.renderManifestFile()).contains("renderMode"));
    }
}
