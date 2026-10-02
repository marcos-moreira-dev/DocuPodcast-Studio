package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectProjectIntegrityUseCaseTest {
    @TempDir
    Path tempDir;

    private final InspectProjectIntegrityUseCase useCase = new InspectProjectIntegrityUseCase();

    @Test
    void coherentProjectWithChecksumLayerAndStoryboardIsOk() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Files.createDirectories(tempDir.resolve("media/images"));
        Files.writeString(tempDir.resolve("media/images/scene.png"), "imagen");

        ProjectAssetReference image = new ProjectAssetReference(
                "IMG-001",
                ProjectAssetKind.IMAGE,
                "Escena",
                "media/images/scene.png",
                "image/png",
                "Imagen asociada",
                "sha256:" + sha256(tempDir.resolve("media/images/scene.png")),
                ""
        );
        DocuPodcastProject base = DocuPodcastProject.createNew("Proyecto íntegro");
        DocuPodcastProject project = base
                .withMetadata(base.metadata().withKind(ProjectKind.STORYBOARD))
                .withAsset(image)
                .withAsset(asset("SCRIPT-001", ProjectAssetKind.NARRATION_SCRIPT, "script/narration-script.json", ""))
                .withAsset(asset("STORYBOARD-001", ProjectAssetKind.STORYBOARD_MANIFEST, "storyboard/storyboard.json", ""))
                .withNarrativeLayerAssignment(new NarrativeLayerAssignment(
                        "LAY-IMG-001",
                        NarrativeLayerKind.IMAGE,
                        new ScriptTextRange("SEG-001", 0, 5),
                        new DocumentTextRange("BLK-001", 0, 5),
                        "IMG-001",
                        "Escena",
                        "Imagen elegida por el usuario"));
        Files.createDirectories(tempDir.resolve("script"));
        Files.createDirectories(tempDir.resolve("storyboard"));
        Files.writeString(tempDir.resolve("script/narration-script.json"), "{}");
        Files.writeString(tempDir.resolve("storyboard/storyboard.json"), "{}");

        ProjectIntegrityReport report = useCase.inspect(project, projectFile, hydration(), List.of());

        assertEquals(ProjectIntegrityStatus.OK, report.status(), report.messages().toString());
        assertTrue(report.messages().isEmpty());
    }

    @Test
    void reportsChecksumMismatchAsRepairRequired() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        Files.createDirectories(tempDir.resolve("media/images"));
        Files.writeString(tempDir.resolve("media/images/scene.png"), "imagen modificada");
        DocuPodcastProject project = DocuPodcastProject.createNew("Checksum")
                .withAsset(asset("IMG-001", ProjectAssetKind.IMAGE, "media/images/scene.png",
                        "sha256:0000000000000000000000000000000000000000000000000000000000000000"));

        ProjectIntegrityReport report = useCase.inspect(project, projectFile, ProjectWorkspaceHydration.empty(), List.of());

        assertTrue(report.requiresRepair());
        assertTrue(report.messages().stream().anyMatch(message -> message.contains("ASSET_CHECKSUM_MISMATCH")));
    }

    @Test
    void reportsNarrativeLayerTargetsThatDoNotExist() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        DocuPodcastProject project = DocuPodcastProject.createNew("Capas")
                .withNarrativeLayerAssignment(new NarrativeLayerAssignment(
                        "LAY-AUDIO-001",
                        NarrativeLayerKind.HUMAN_AUDIO,
                        new ScriptTextRange("SEG-001", 0, 10),
                        new DocumentTextRange("BLK-001", 0, 10),
                        "AUD-MISSING",
                        "Lucía hablando",
                        "El usuario eligió un audio externo"))
                .withNarrativeLayerAssignment(new NarrativeLayerAssignment(
                        "LAY-EMO-001",
                        NarrativeLayerKind.EMOTION,
                        new ScriptTextRange("SEG-001", 0, 10),
                        new DocumentTextRange("BLK-001", 0, 10),
                        "STY-MISSING",
                        "Triste",
                        "Estilo inexistente"));

        ProjectIntegrityReport report = useCase.inspect(project, projectFile, hydration(), List.of());

        assertTrue(report.requiresRepair());
        assertTrue(report.messages().stream().anyMatch(message -> message.contains("LAYER_TARGET_AUDIO_MISSING")));
        assertTrue(report.messages().stream().anyMatch(message -> message.contains("LAYER_TARGET_STYLE_MISSING")));
    }

    @Test
    void audioJobMissingCompletedWavRequiresRepair() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        AudioSegmentSnapshot completedButMissing = AudioSegmentSnapshot.pending("SEG-001", "Inicio")
                .completed("jobs/JOB-001/audio/SEG-001.wav", 3.0);
        AudioJobSnapshot job = new AudioJobSnapshot(
                "JOB-001",
                "Documento",
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                1,
                1,
                0,
                1.0,
                "SEG-001",
                "Inicio",
                0,
                "Completo pero falta archivo físico",
                "jobs/JOB-001",
                "jobs/JOB-001/final.wav",
                "jobs/JOB-001/playback-manifest.json",
                List.of(completedButMissing),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        ProjectIntegrityReport report = useCase.inspect(DocuPodcastProject.createNew("Audio"), projectFile,
                ProjectWorkspaceHydration.empty(), List.of(job));

        assertTrue(report.requiresRepair());
        assertTrue(report.messages().stream().anyMatch(message -> message.contains("AUDIO_JOB_REQUIRES_REPAIR")));
    }

    @Test
    void supersededFailedAudioJobsDoNotWarnWhileReplacementIsRunning() throws Exception {
        Path projectFile = tempDir.resolve("demo.docupodcast.json");
        Files.writeString(projectFile, "{}");
        AudioSegmentSnapshot pending = AudioSegmentSnapshot.pending("SEG-001", "Inicio");
        AudioJobSnapshot failed = new AudioJobSnapshot(
                "JOB-OLD",
                "Documento",
                AudioJobState.FAILED,
                AudioGenerationStage.FAILED,
                0,
                1,
                1,
                0.0,
                "SEG-001",
                "Inicio",
                0,
                "Fallo anterior",
                "jobs/JOB-OLD",
                "",
                "",
                List.of(pending),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:01:00Z")
        );
        AudioJobSnapshot running = new AudioJobSnapshot(
                "JOB-NEW",
                "Documento",
                AudioJobState.GENERATING_AUDIO,
                AudioGenerationStage.GENERATING_SEGMENTS,
                0,
                1,
                0,
                0.0,
                "SEG-001",
                "Inicio",
                10,
                "Generando",
                "jobs/JOB-NEW",
                "",
                "",
                List.of(pending),
                Instant.parse("2026-01-01T00:02:00Z"),
                Instant.parse("2026-01-01T00:03:00Z")
        );

        ProjectIntegrityReport report = useCase.inspect(
                DocuPodcastProject.createNew("Audio"),
                projectFile,
                ProjectWorkspaceHydration.empty(),
                List.of(failed, running));

        assertTrue(report.ok());
    }

    private static ProjectWorkspaceHydration hydration() {
        return new ProjectWorkspaceHydration(
                Optional.of(new com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource(document())),
                Optional.of(script()),
                Optional.of(storyboard())
        );
    }

    private static ReadableDocument document() {
        return new ReadableDocument(
                "Documento",
                SourceDocumentFormat.DOCX,
                Path.of("documento.docx"),
                List.of(DocumentBlock.of("BLK-001", DocumentBlockType.PARAGRAPH, "Hola mundo narrable", "Normal")),
                DocumentImportReport.empty()
        );
    }

    private static NarrationScriptDocument script() {
        return new NarrationScriptDocument(
                "SCRIPT-001",
                "Guion",
                "es",
                "Documento",
                List.of(NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Inicio", "Hola mundo narrable", List.of("BLK-001"))),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );
    }

    private static StoryboardDocument storyboard() {
        return new StoryboardDocument(
                "STORYBOARD-001",
                "Storyboard",
                "SCRIPT-001",
                List.of(new StoryboardBinding("BND-001", "SEG-001", "IMG-001", StoryboardDisplayMode.FIT_CONTAIN, "", Map.of())),
                Map.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                ""
        );
    }

    private static ProjectAssetReference asset(String id, ProjectAssetKind kind, String relativePath, String checksum) {
        return new ProjectAssetReference(id, kind, id, relativePath, "application/octet-stream", "Prueba", checksum, "");
    }

    private static String sha256(Path file) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
