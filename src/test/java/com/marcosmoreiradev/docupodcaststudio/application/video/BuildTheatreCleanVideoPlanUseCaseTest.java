package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildTheatreCleanVideoPlanUseCaseTest {
    @TempDir Path projectDirectory;

    @Test
    void usesOriginalImageAndEmitsAtMostOneFramePerUniqueAudioUnit() throws Exception {
        Path image = projectDirectory.resolve("media/images/frame.png");
        Path audio1 = projectDirectory.resolve("jobs/JOB/audio/SEG-001-U001.wav");
        Path audio2 = projectDirectory.resolve("jobs/JOB/audio/SEG-001-U002.wav");
        Files.createDirectories(image.getParent());
        Files.createDirectories(audio1.getParent());
        Files.write(image, new byte[] {1});
        Files.write(audio1, new byte[] {1});
        Files.write(audio2, new byte[] {1});

        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Texto", "Hola mundo", List.of("B0001"))));
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE, "Frame",
                        "media/images/frame.png", "image/png", "frame", "", ""))
                .withTheatre(theatre());
        AudioJobSnapshot job = job(List.of(unit("SEG-001-U001", 1.2), unit("SEG-001-U002", 1.4)));

        SimpleVideoPlan plan = new BuildTheatreCleanVideoPlanUseCase().build(
                project, script, StoryboardDocument.createForScript(script), List.of(job), projectDirectory,
                SimpleVideoExportSettings.defaults());

        assertEquals(2, plan.frameCount());
        assertEquals(List.of("SEG-001-U001", "SEG-001-U002"),
                plan.frames().stream().map(SimpleVideoFrame::segmentId).toList());
        assertTrue(plan.frames().stream().allMatch(frame -> frame.imageRelativePath().equals("media/images/frame.png")));
        assertTrue(plan.frames().stream().noneMatch(frame -> frame.imageRelativePath().endsWith(".generated.png")));
    }

    @Test
    void includesPersistedIntermediateFrameAsSecondVisualHalfWhenEnabled() throws Exception {
        Path imageA = projectDirectory.resolve("media/images/a.png");
        Path imageB = projectDirectory.resolve("media/images/b.png");
        Path inferred = projectDirectory.resolve("media/images/a-to-b.png");
        Path audio1 = projectDirectory.resolve("jobs/JOB/audio/SEG-001.wav");
        Path audio2 = projectDirectory.resolve("jobs/JOB/audio/SEG-002.wav");
        Files.createDirectories(imageA.getParent());
        Files.createDirectories(audio1.getParent());
        Files.write(imageA, new byte[] {1});
        Files.write(imageB, new byte[] {1});
        Files.write(inferred, new byte[] {1});
        Files.write(audio1, new byte[] {1});
        Files.write(audio2, new byte[] {1});

        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Hola", List.of("B0001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos", "Adios", List.of("B0002"))));
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("IMG-A", ProjectAssetKind.IMAGE, "A",
                        "media/images/a.png", "image/png", "frame", "", ""))
                .withAsset(new ProjectAssetReference("IMG-B", ProjectAssetKind.IMAGE, "B",
                        "media/images/b.png", "image/png", "frame", "", ""))
                .withAsset(new ProjectAssetReference("IMG-MID", ProjectAssetKind.IMAGE, "A-B",
                        "media/images/a-to-b.png", "image/png", "frame", "", ""))
                .withTheatre(theatreWithIntermediate());
        AudioJobSnapshot job = job(List.of(unit("SEG-001", 2.0), unit("SEG-002", 2.0)));
        SimpleVideoExportSettings settings = SimpleVideoExportSettings.defaults()
                .withIncludeInferredFrames(true);

        SimpleVideoPlan plan = new BuildTheatreCleanVideoPlanUseCase().build(
                project, script, StoryboardDocument.createForScript(script), List.of(job), projectDirectory, settings);

        SimpleVideoFrame first = plan.frames().getFirst();
        assertEquals(0.25, first.silenceAfterSeconds(), 0.0001);
        assertEquals(2.25, first.frameDurationSeconds(), 0.0001);
        assertEquals(2, first.visualParts().size());
        assertEquals("media/images/a.png", first.visualParts().get(0).imageRelativePath());
        assertEquals("media/images/a-to-b.png", first.visualParts().get(1).imageRelativePath());
        assertEquals(first.frameDurationSeconds() / 2.0, first.visualParts().get(0).durationSeconds(), 0.0001);
        assertEquals(first.frameDurationSeconds() / 2.0, first.visualParts().get(1).durationSeconds(), 0.0001);
    }

    @Test
    void exportUsesAssignedOfficialImageBeforeGeneratedVariant() throws Exception {
        Path official = projectDirectory.resolve("media/images/oficial.png");
        Path generated = projectDirectory.resolve("media/images/generada.png");
        Path audio = projectDirectory.resolve("jobs/JOB/audio/SEG-001.wav");
        Files.createDirectories(official.getParent());
        Files.createDirectories(audio.getParent());
        Files.write(official, new byte[] {1});
        Files.write(generated, new byte[] {1});
        Files.write(audio, new byte[] {1});

        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Hola", List.of("B0001"))));
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("IMG-OFFICIAL", ProjectAssetKind.IMAGE, "Oficial",
                        "media/images/oficial.png", "image/png", "frame", "", ""))
                .withAsset(new ProjectAssetReference("IMG-GENERATED", ProjectAssetKind.IMAGE, "Generada",
                        "media/images/generada.png", "image/png", "frame", "", ""))
                .withTheatre(theatre());
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script).withBinding(new StoryboardBinding(
                "STB-001",
                "SEG-001",
                "IMG-GENERATED",
                StoryboardDisplayMode.FIT_CONTAIN,
                "Generada",
                Map.of(
                        UpsertTheatreStoryboardFrameVariantUseCase.OFFICIAL_IMAGE_ASSET_ID, "IMG-OFFICIAL",
                        UpsertTheatreStoryboardFrameVariantUseCase.GENERATED_IMAGE_ASSET_ID, "IMG-GENERATED",
                        UpsertTheatreStoryboardFrameVariantUseCase.ACTIVE_VISUAL_VARIANT,
                        UpsertTheatreStoryboardFrameVariantUseCase.VARIANT_GENERATED)));

        SimpleVideoPlan plan = new BuildTheatreCleanVideoPlanUseCase().build(
                project, script, storyboard, List.of(job(List.of(unit("SEG-001", 2.0)))), projectDirectory,
                SimpleVideoExportSettings.defaults());

        assertEquals("IMG-OFFICIAL", plan.frames().getFirst().imageAssetId());
        assertEquals("media/images/oficial.png", plan.frames().getFirst().imageRelativePath());
    }

    @Test
    void disabledIntermediateCheckboxKeepsSingleVisualPart() throws Exception {
        Path imageA = projectDirectory.resolve("media/images/a.png");
        Path imageB = projectDirectory.resolve("media/images/b.png");
        Path inferred = projectDirectory.resolve("media/images/a-to-b.png");
        Path audio1 = projectDirectory.resolve("jobs/JOB/audio/SEG-001.wav");
        Path audio2 = projectDirectory.resolve("jobs/JOB/audio/SEG-002.wav");
        Files.createDirectories(imageA.getParent());
        Files.createDirectories(audio1.getParent());
        Files.write(imageA, new byte[] {1});
        Files.write(imageB, new byte[] {1});
        Files.write(inferred, new byte[] {1});
        Files.write(audio1, new byte[] {1});
        Files.write(audio2, new byte[] {1});

        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Hola", List.of("B0001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos", "Adios", List.of("B0002"))));
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("IMG-A", ProjectAssetKind.IMAGE, "A",
                        "media/images/a.png", "image/png", "frame", "", ""))
                .withAsset(new ProjectAssetReference("IMG-B", ProjectAssetKind.IMAGE, "B",
                        "media/images/b.png", "image/png", "frame", "", ""))
                .withAsset(new ProjectAssetReference("IMG-MID", ProjectAssetKind.IMAGE, "A-B",
                        "media/images/a-to-b.png", "image/png", "frame", "", ""))
                .withTheatre(theatreWithIntermediate());
        AudioJobSnapshot job = job(List.of(unit("SEG-001", 2.0), unit("SEG-002", 2.0)));

        SimpleVideoPlan plan = new BuildTheatreCleanVideoPlanUseCase().build(
                project, script, StoryboardDocument.createForScript(script), List.of(job), projectDirectory,
                SimpleVideoExportSettings.defaults());

        assertEquals(1, plan.frames().getFirst().visualParts().size());
        assertEquals("media/images/a.png", plan.frames().getFirst().visualParts().getFirst().imageRelativePath());
    }

    @Test
    void missingVisualIsReportedAndCanUseBlackPlaceholderWhenExplicitlyEnabled() throws Exception {
        Path audio = projectDirectory.resolve("jobs/JOB/audio/SEG-001-U001.wav");
        Files.createDirectories(audio.getParent());
        Files.write(audio, new byte[] {1});
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH,
                        "Texto", "Hola mundo", List.of("B0001"))));
        DocuPodcastProject project = DocuPodcastProject.empty("Demo");
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script);
        BuildTheatreCleanVideoPlanUseCase useCase = new BuildTheatreCleanVideoPlanUseCase();

        assertEquals(List.of("SEG-001"),
                useCase.missingVisualSegmentIds(project, script, storyboard, projectDirectory));
        assertThrows(java.io.IOException.class, () -> useCase.build(project, script, storyboard,
                List.of(job(List.of(unit("SEG-001-U001", 1.2)))), projectDirectory,
                SimpleVideoExportSettings.defaults()));

        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.HD_720, 30, 0.0, true, true).withRenderUnassignedVisuals(true);
        SimpleVideoPlan plan = useCase.build(project, script, storyboard,
                List.of(job(List.of(unit("SEG-001-U001", 1.2)))), projectDirectory, settings);
        Path placeholder = projectDirectory.resolve(plan.frames().getFirst().imageRelativePath());
        var image = ImageIO.read(placeholder.toFile());

        assertEquals(1280, image.getWidth());
        assertEquals(720, image.getHeight());
        assertEquals(0x000000, image.getRGB(5, 5) & 0xFFFFFF);
    }

    private static TheatreProjectLayer theatre() {
        return new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B0001")),
                List.of(), List.of(), List.of(),
                List.of(new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-001", "")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static TheatreProjectLayer theatreWithIntermediate() {
        return new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B0001"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B0002")),
                List.of(), List.of(), List.of(),
                List.of(
                        new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-A", ""),
                        new TheatreProjectLayer.IntervencionVisual("INTERVENCION-2", "IMG-B", "")),
                List.of(new TheatreProjectLayer.IntermediateFrame("INTERVENCION-1", "INTERVENCION-2", "IMG-MID", "")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static AudioSegmentSnapshot unit(String id, double duration) {
        return new AudioSegmentSnapshot(id, id, AudioSegmentStatus.COMPLETED,
                "jobs/JOB/audio/" + id + ".wav", duration, 1, "");
    }

    private static AudioJobSnapshot job(List<AudioSegmentSnapshot> segments) {
        return new AudioJobSnapshot("JOB", "Demo", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                segments.size(), segments.size(), 0, 1.0, "", "", 0, "OK", "jobs/JOB", "",
                "jobs/JOB/audio-manifest.json", segments, Instant.now(), Instant.now());
    }
}
