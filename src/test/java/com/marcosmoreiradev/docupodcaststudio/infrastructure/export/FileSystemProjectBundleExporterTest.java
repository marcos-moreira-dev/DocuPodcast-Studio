package com.marcosmoreiradev.docupodcaststudio.infrastructure.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.ProjectBundleExportRequest;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMetadata;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileSystemProjectBundleExporterTest {
    @TempDir
    Path temp;

    @Test
    void exportsInputEditableOutputJobsAndReports() throws Exception {
        Path projectFile = temp.resolve("MiProyecto.docupodcast.json");
        Files.writeString(projectFile, "{\"formatVersion\":1}");
        Files.createDirectories(temp.resolve("source"));
        Files.writeString(temp.resolve("source/notas.docx"), "fake docx");
        Files.createDirectories(temp.resolve("assets/storyboard"));
        Files.writeString(temp.resolve("assets/storyboard/escena.png"), "png");
        Files.createDirectories(temp.resolve("jobs/JOB-001/final"));
        Files.writeString(temp.resolve("jobs/JOB-001/final/podcast.wav"), "wav");
        Files.writeString(temp.resolve("jobs/JOB-001/job.json"), "{}{}");
        DocuPodcastProject project = new DocuPodcastProject(
                ProjectMetadata.create("Mi Proyecto"),
                new ProjectAssetCatalog(List.of(
                        ProjectAssetReference.sourceDocument("SRC-001", "Notas", "source/notas.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                        new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE, "Escena", "assets/storyboard/escena.png", "image/png", "Storyboard", "", "")
                )),
                Map.of());
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "notas.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto", List.of())
        ));
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-001", "Guion", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "SEG-001", "Intro", 0, "OK",
                "jobs/JOB-001", "jobs/JOB-001/final/podcast.wav", "jobs/JOB-001/audio-manifest.json",
                List.of(), Instant.now(), Instant.now());

        var result = new FileSystemProjectBundleExporter().export(new ProjectBundleExportRequest(
                project, projectFile, script, null, List.of(job), temp.resolve("exports")));

        assertTrue(Files.exists(result.rootDirectory().resolve("input/notas.docx")));
        assertTrue(Files.exists(result.rootDirectory().resolve("editable/MiProyecto.docupodcast.json")));
        assertTrue(Files.exists(result.rootDirectory().resolve("output/lectura_preparada.md")));
        assertTrue(Files.exists(result.rootDirectory().resolve("output/podcast.wav")));
        assertTrue(Files.exists(result.rootDirectory().resolve("output/podcast.export-report.md")));
        assertTrue(Files.exists(result.rootDirectory().resolve("jobs/jobs/JOB-001/job.json")));
        assertTrue(Files.exists(result.rootDirectory().resolve("assets/registered/source_document/source/notas.docx")));
        assertTrue(Files.exists(result.rootDirectory().resolve("assets/registered/image/assets/storyboard/escena.png")));
        assertTrue(Files.exists(result.manifestFile()));
        assertTrue(Files.exists(result.readmeFile()));
        assertTrue(Files.exists(result.fileIndexFile()));
        assertTrue(Files.exists(result.exportReadinessFile()));
        assertFalse(result.artifacts().isEmpty());

        String manifest = Files.readString(result.manifestFile());
        String index = Files.readString(result.fileIndexFile());
        String readiness = Files.readString(result.exportReadinessFile());
        assertTrue(manifest.contains("## Índice SHA-256"));
        assertTrue(manifest.contains("Assets copiados: 2"));
        assertTrue(manifest.contains("Estado exportaciones"));
        assertTrue(readiness.contains("Preparación de exportaciones DocuPodcast"));
        assertTrue(readiness.contains("Podcast WAV"));
        assertTrue(index.contains("relative_path\tsize_bytes\tsha256"));
        assertTrue(index.contains("output/lectura_preparada.md"));
        assertTrue(index.contains("podcast.wav"));
        assertTrue(index.contains("podcast.export-report.md"));
    }
}
