package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeGeneratedClip;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeKeyframeSource;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeParagraphTake;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectExportReadinessUseCaseTest {
    private final InspectExportReadinessUseCase useCase = new InspectExportReadinessUseCase();
    @TempDir
    Path tempDir;

    @Test
    void reportDeclaresExportableAndBlockedOutputsWithoutUi() {
        ExportReadinessReport report = useCase.inspect(
                DocuPodcastProject.createNew("Demo"),
                Path.of("demo.docupodcast.json"),
                script(),
                null,
                List.of());

        assertEquals(ExportReadinessStatus.EXPORTABLE_CON_ADVERTENCIAS, report.status());
        assertTrue(report.exportableCount() >= 3);
        assertTrue(report.blockedCount() >= 1);
        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.PODCAST_WAV
                && item.status() == ExportReadinessStatus.BLOQUEADO));
        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE
                && item.status() == ExportReadinessStatus.EXPORTABLE_CON_ADVERTENCIAS));
        assertTrue(report.toMarkdown().contains("qué puede salir") || report.toMarkdown().contains("Salidas exportables"));
    }

    @Test
    void fullBrainStateMakesPodcastStoryboardAndVideoExportable() {
        ExportReadinessReport report = useCase.inspect(
                DocuPodcastProject.createNew("Completo"),
                Path.of("completo.docupodcast.json"),
                script(),
                storyboard(),
                List.of(completedJob()));

        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.PODCAST_WAV
                && item.status() == ExportReadinessStatus.EXPORTABLE));
        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.STORYBOARD_SUMMARY
                && item.status() == ExportReadinessStatus.EXPORTABLE));
        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE
                && item.status() == ExportReadinessStatus.EXPORTABLE));
        assertTrue(report.toMarkdown().contains("Limitaciones honestas"));
    }

    @Test
    void unsavedProjectBlocksBundleAndCreativeOutputsThatNeedProjectFolder() {
        ExportReadinessReport report = useCase.inspect(
                DocuPodcastProject.createNew("Sin guardar"),
                null,
                script(),
                storyboard(),
                List.of(completedJob()));

        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.PROJECT_BUNDLE
                && item.blocked()));
        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.PODCAST_WAV
                && item.blocked()
                && item.missingRequirements().stream().anyMatch(text -> text.contains("Guarda el proyecto"))));
        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.DIAGNOSTIC_REPORT
                && item.exportable()));
    }

    @Test
    void narrativeVideoMp4RequiresCurrentKeyframeClipsAndAudioInsideProject() throws Exception {
        ExportReadinessReport blocked = useCase.inspect(
                DocuPodcastProject.createNew("Video", ProjectMode.NARRATIVE_VIDEO),
                tempDir.resolve("video.docupodcast.json"),
                script(),
                null,
                List.of(completedJob()));

        assertTrue(blocked.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.FINAL_VIDEO_MP4
                && item.blocked()
                && item.missingRequirements().stream().anyMatch(text -> text.contains("imagen clave"))));

        Files.createDirectories(tempDir.resolve("generated/narrative/keyframes"));
        Files.createDirectories(tempDir.resolve("generated/narrative/clips"));
        Files.createDirectories(tempDir.resolve("jobs/JOB-001/audio"));
        Files.write(tempDir.resolve("generated/narrative/keyframes/BLK-001.png"), new byte[] { 1 });
        Files.write(tempDir.resolve("generated/narrative/clips/BLK-001-001.mp4"), new byte[] { 2 });
        Files.write(tempDir.resolve("jobs/JOB-001/audio/SEG-001.wav"), new byte[] { 3 });
        ExportReadinessReport exportable = useCase.inspect(
                narrativeProjectWithGeneratedTake(),
                tempDir.resolve("video.docupodcast.json"),
                script(),
                null,
                List.of(completedJob()));

        assertTrue(exportable.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.FINAL_VIDEO_MP4
                && item.exportable()
                && item.evidence().stream().anyMatch(text -> text.contains("parte(s) de video local"))));
    }

    @Test
    void documentaryTextAudioVideoRequiresPreparedReadingSavedProjectAndAudio() {
        ExportReadinessReport blocked = useCase.inspect(
                DocuPodcastProject.createNew("Estudio", ProjectMode.DOCUMENTARY_STUDIO),
                Path.of("estudio.docupodcast.json"),
                script(),
                null,
                List.of());

        assertTrue(blocked.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO
                && item.blocked()
                && item.missingRequirements().stream().anyMatch(text -> text.contains("Falta audio"))));

        ExportReadinessReport exportable = useCase.inspect(
                DocuPodcastProject.createNew("Estudio", ProjectMode.DOCUMENTARY_STUDIO),
                Path.of("estudio.docupodcast.json"),
                script(),
                null,
                List.of(completedJob()));

        assertTrue(exportable.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO
                && item.exportable()));
    }

    @Test
    void theatreModeAddsSpecificWorkMapAndPortionReadiness() {
        ExportReadinessReport blocked = useCase.inspect(
                theatreProjectWithMap(),
                Path.of("obra.docupodcast.json"),
                script(),
                null,
                List.of());

        assertTrue(blocked.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_WORK_VIDEO
                && item.blocked()
                && item.missingRequirements().stream().anyMatch(text -> text.contains("Falta audio"))));
        assertTrue(blocked.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO));
        assertTrue(blocked.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_PORTION_VIDEO));

        ExportReadinessReport exportable = useCase.inspect(
                theatreProjectWithMap(),
                Path.of("obra.docupodcast.json"),
                script(),
                null,
                List.of(completedJob()));

        assertTrue(exportable.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_WORK_VIDEO
                && item.exportable()
                && item.evidence().stream().anyMatch(text -> text.contains("1 intervencion"))));
        assertTrue(exportable.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO
                && item.exportable()
                && item.evidence().stream().anyMatch(text -> text.contains("placement"))));
        assertTrue(exportable.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_PORTION_VIDEO
                && item.exportable()));
    }

    @Test
    void theatreSpatialMapBlocksWithoutMinimumMapData() {
        ExportReadinessReport report = useCase.inspect(
                theatreProjectWithoutMapData(),
                Path.of("obra.docupodcast.json"),
                script(),
                null,
                List.of(completedJob()));

        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_WORK_VIDEO
                && item.exportable()));
        assertTrue(report.items().stream().anyMatch(item -> item.kind() == ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO
                && item.blocked()
                && item.missingRequirements().stream().anyMatch(text -> text.contains("posiciones o acciones"))));
    }

    private static NarrationScriptDocument script() {
        return new NarrationScriptDocument(
                "SCRIPT-001",
                "Guion",
                "es",
                "demo.docx",
                List.of(NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Hola mundo narrable", List.of("BLK-001"))),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                "");
    }

    private static StoryboardDocument storyboard() {
        return new StoryboardDocument(
                "STORYBOARD-001",
                "Storyboard",
                "SCRIPT-001",
                List.of(StoryboardBinding.of("BIND-001", "SEG-001", "IMG-001", "Escena inicial")),
                Map.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                "");
    }

    private static DocuPodcastProject narrativeProjectWithGeneratedTake() {
        NarrativeGeneratedClip clip = new NarrativeGeneratedClip(
                "CLIP-001", "VIDEO-001", 0, 3.0, "", "wan22", "workflow", 42L,
                "fingerprint", Map.of());
        NarrativeParagraphTake take = new NarrativeParagraphTake(
                "BLK-001", true, "IMG-001", NarrativeKeyframeSource.GENERATED,
                List.of(clip), "prompt", "", 42L, "fingerprint", false, "");
        return DocuPodcastProject.createNew("Video", ProjectMode.NARRATIVE_VIDEO)
                .withAsset(new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE, "Escena",
                        "generated/narrative/keyframes/BLK-001.png", "image/png", "Imagen clave", "", ""))
                .withAsset(new ProjectAssetReference("VIDEO-001", ProjectAssetKind.VIDEO_SOURCE, "Clip",
                        "generated/narrative/clips/BLK-001-001.mp4", "video/mp4", "Clip narrativo", "", ""))
                .withNarrative(DocuPodcastProject.createNew("Video", ProjectMode.NARRATIVE_VIDEO)
                        .narrative().withTake(take));
    }

    private static DocuPodcastProject theatreProjectWithMap() {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "BLK-001")),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-ACTOR", "Actor", List.of("ACTOR"), "")),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "Acto 1", "")),
                List.of(new TheatreProjectLayer.Scene("SCN-001", "Escena 1", "", "ACT-001", "")),
                List.of(new TheatreProjectLayer.SpatialPosition("SCN-001", "INTERVENCION-1", "CHR-ACTOR", 0.5, 0.5, "")),
                List.of(new TheatreProjectLayer.TheatreAction("SCN-001", "INTERVENCION-1", "INTERVENCION-1", "CHR-ACTOR", "Se dirige al publico", true)),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SCN-001", "CHR-ACTOR", "centro", "publico", "Publico", Map.of())),
                List.of(),
                List.of());
        return DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION).withTheatre(theatre);
    }

    private static DocuPodcastProject theatreProjectWithoutMapData() {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "BLK-001")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "Acto 1", "")),
                List.of(new TheatreProjectLayer.Scene("SCN-001", "Escena 1", "", "ACT-001", "")),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SCN-001", "", "centro", "", "", Map.of())),
                List.of(),
                List.of());
        return DocuPodcastProject.createNew("Obra", ProjectMode.THEATRE_PRODUCTION).withTheatre(theatre);
    }

    private static AudioJobSnapshot completedJob() {
        AudioSegmentSnapshot segment = AudioSegmentSnapshot.pending("SEG-001", "Intro")
                .completed("jobs/JOB-001/audio/SEG-001.wav", 3.0);
        return new AudioJobSnapshot(
                "JOB-001",
                "Guion",
                AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY,
                1,
                1,
                0,
                1.0,
                "SEG-001",
                "Intro",
                0,
                "OK",
                "jobs/JOB-001",
                "",
                "jobs/JOB-001/playback-manifest.json",
                List.of(segment),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
