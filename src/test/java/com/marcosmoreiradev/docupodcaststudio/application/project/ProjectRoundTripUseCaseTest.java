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
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.audio.AudioJobFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.ReadableDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.DocuPodcastProjectFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.script.NarrationScriptWorkspaceFileRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard.StoryboardWorkspaceFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRoundTripUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsReopensNarratedDocumentLayersStoryboardAndAudioJobs() throws Exception {
        Path sourceDocx = tempDir.resolve("documento-fuente.docx");
        Files.writeString(sourceDocx, "contenido fuente solo lectura");
        Files.createDirectories(tempDir.resolve("media/images"));
        Files.writeString(tempDir.resolve("media/images/escena-001.png"), "imagen de prueba");

        ReadableDocument document = new ReadableDocument(
                "Documento narrable",
                SourceDocumentFormat.DOCX,
                sourceDocx,
                List.of(DocumentBlock.of("BLK-001", DocumentBlockType.PARAGRAPH,
                        "Este texto se lee mientras el usuario sigue el documento.", "Normal")),
                DocumentImportReport.empty()
        );
        NarrationScriptDocument narrationProjection = NarrationScriptDocument.create(
                "Proyección interna",
                "es",
                document.title(),
                List.of(NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Inicio",
                        "Este texto se lee mientras el usuario sigue el documento.", List.of("BLK-001")))
        );
        StoryboardDocument storyboard = StoryboardDocument.createForScript(narrationProjection)
                .withBinding(StoryboardBinding.of("BND-001", "SEG-001", "IMG-001", "Imagen de apoyo"));

        ProjectAssetReference image = new ProjectAssetReference(
                "IMG-001",
                ProjectAssetKind.IMAGE,
                "Imagen de apoyo",
                "media/images/escena-001.png",
                "image/png",
                "Imagen asociada a texto narrado",
                "",
                "Debe poder reutilizarse sin duplicar el asset"
        );
        NarrativeLayerAssignment imageLayer = new NarrativeLayerAssignment(
                "LAY-IMG-001",
                NarrativeLayerKind.IMAGE,
                new ScriptTextRange("SEG-001", 0, 24),
                new DocumentTextRange("BLK-001", 0, 24),
                "IMG-001",
                "Imagen de apoyo",
                "La capa vive en el proyecto; el DOCX no se modifica"
        );
        DocuPodcastProject project = DocuPodcastProject.createNew("Round-trip mínimo")
                .withAsset(image)
                .withNarrativeLayerAssignment(imageLayer);

        Instant now = Instant.now();
        AudioSegmentSnapshot segment = AudioSegmentSnapshot.pending("SEG-001", "Inicio")
                .completed("jobs/JOB-001/audio/SEG-001.wav", 3.2);
        AudioJobSnapshot audioJob = new AudioJobSnapshot(
                "JOB-001",
                document.title(),
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                1,
                1,
                0,
                1.0,
                "SEG-001",
                "Inicio",
                0,
                "Audio listo para reabrir",
                "jobs/JOB-001",
                "jobs/JOB-001/final.wav",
                "jobs/JOB-001/playback-manifest.json",
                List.of(segment),
                now,
                now
        );

        ProjectRoundTripUseCase useCase = new ProjectRoundTripUseCase(
                new DocuPodcastProjectFileRepository(),
                new ReadableDocumentWorkspaceRepository(),
                new NarrationScriptWorkspaceFileRepository(),
                new StoryboardWorkspaceFileRepository(),
                new AudioJobFileRepository()
        );

        ProjectRoundTripResult result = useCase.execute(ProjectRoundTripRequest.of(
                project,
                tempDir.resolve("roundtrip.docupodcast.json"),
                document,
                narrationProjection,
                storyboard,
                List.of(audioJob)
        ));

        assertTrue(result.successful(), result.summary() + " " + result.messages());
        assertTrue(result.hydration().importedDocument().isPresent());
        assertTrue(result.hydration().narrationScript().isPresent());
        assertTrue(result.hydration().storyboard().isPresent());
        assertEquals(1, result.restoredProject().narrativeLayerAssignments().size());
        assertTrue(result.restoredProject().assets().byId("IMG-001").isPresent());
        assertEquals(1, result.restoredAudioJobs().size());
    }
}
