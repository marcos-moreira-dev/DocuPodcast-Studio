package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;

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
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildTheatreSpatialVideoPlanUseCaseTest {
    @Test
    void stageDirectionUsesItsGeneratedAudioAndDoesNotAddSilentVisualDelay() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005.wav");
        NarrationSegment stageDirection = new NarrationSegment(
                "SEG-005", NarrationSegmentType.PARAGRAPH, "Acotación",
                "Acotación: La plaza queda a oscuras y el narrador sale de escena.",
                List.of("B0005"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("theatreStageDirection", "true"));

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP"), script(List.of(stageDirection)), List.of(job()), tempDir,
                new SimpleVideoExportSettings(SimpleVideoResolutionPreset.HD_720, 30, 5.0, false, true,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.AUTO,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.CPU_X264),
                "fragments");

        SimpleVideoFrame frame = plan.frames().getFirst();
        assertEquals("jobs/JOB-001/audio/SEG-005.wav", frame.audioRelativePath());
        assertTrue(frame.audioReady());
        assertFalse(frame.silentVisual());
        assertEquals(0.25, frame.silenceAfterSeconds(), 0.0001);
        assertEquals(2.25, frame.frameDurationSeconds(), 0.0001);
        BufferedImage rendered = ImageIO.read(tempDir.resolve(frame.imageRelativePath()).toFile());
        Color formerCompanionArea = new Color(rendered.getRGB(rendered.getWidth() / 4, rendered.getHeight() / 2));
        assertTrue(isWhiteish(formerCompanionArea),
                "Una acotación no debe mostrar una tarjeta de personaje; el mapa debe ocupar el ancho disponible.");
    }

    @Test
    void stageDirectionWithoutAudioGetsEnoughSilentReadingTime() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        String text = "Acotación: " + "La escena cambia lentamente mientras todos observan el nuevo espacio. ".repeat(5);
        NarrationSegment stageDirection = new NarrationSegment(
                "SEG-005", NarrationSegmentType.PARAGRAPH, "Acotación", text,
                List.of("B0005"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("theatreStageDirection", "true"));

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP"), script(List.of(stageDirection)), List.of(), tempDir,
                new SimpleVideoExportSettings(SimpleVideoResolutionPreset.HD_720, 30, 3.0, false, true,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.AUTO,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.CPU_X264),
                "fragments");

        assertTrue(plan.frames().stream().allMatch(SimpleVideoFrame::silentVisual));
        assertTrue(plan.totalDurationSeconds() > 10.0);
    }

    @Test void sceneryExportWorksWithoutSpatialMapAndKeepsAudioDuration() throws Exception {
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005.wav");
        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(project("MISSING-MAP"), script(),
                List.of(job()),tempDir,SimpleVideoExportSettings.defaults(),"scenery");
        var frame=plan.frames().getFirst();
        org.junit.jupiter.api.Assertions.assertFalse(frame.visualParts().isEmpty());
        assertEquals(frame.audioDurationSeconds()+frame.silenceAfterSeconds(),
                frame.visualParts().stream().mapToDouble(SimpleVideoFrame.VisualPart::durationSeconds).sum(),0.001);
    }

    @Test
    void sceneryCaptionUsesAndFitsTheWholeInterventionWhenAudioWasSplitIntoChunks() throws Exception {
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005A.wav");
        createAudio("jobs/JOB-001/audio/SEG-005B.wav");
        String first = "Acotación: " + "La plaza cambia lentamente mientras el pueblo observa. ".repeat(22);
        String second = "El carpintero termina su trabajo y el narrador permanece a un costado. ".repeat(22);
        NarrationSegment firstSegment = new NarrationSegment(
                "SEG-005A", NarrationSegmentType.PARAGRAPH, "Acotación", first,
                List.of("B0005"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("theatreStageDirection", "true"));
        NarrationSegment secondSegment = new NarrationSegment(
                "SEG-005B", NarrationSegmentType.PARAGRAPH, "Acotación", second,
                List.of("B0005"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("theatreStageDirection", "true"));
        AudioJobSnapshot splitJob = new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 2, 2, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-005A", first, AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-005A.wav", 2.0, 1, ""),
                new AudioSegmentSnapshot("SEG-005B", second, AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-005B.wav", 2.0, 1, "")
        ), Instant.now(), Instant.now());

        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.HD_720, 30, 0.5, false, true,
                com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.AUTO,
                com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.CPU_X264);
        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("MISSING-MAP"), script(List.of(firstSegment, secondSegment)), List.of(splitJob), tempDir,
                settings, "scenery");

        String complete = (first.strip() + " " + second.strip()).strip();
        int expectedPages = BuildTheatreSpatialVideoPlanUseCase.sceneryCaptionPages(complete, 1280, 720).size();
        assertTrue(expectedPages > 1, "La prueba debe exigir varias páginas para la intervención completa.");
        assertEquals(2, plan.frameCount());
        assertTrue(plan.frames().stream().allMatch(frame -> frame.narrationPreview().equals(complete)));
        assertTrue(plan.frames().stream().allMatch(frame -> frame.visualParts().size() == expectedPages));
    }

    @TempDir
    Path tempDir;

    @Test
    void buildsTheatreMapFramesFromMdLayerWithoutBurningPlaybar() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005.wav");

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP"),
                script(),
                List.of(job()),
                tempDir,
                new SimpleVideoExportSettings(SimpleVideoResolutionPreset.HD_720, 30, 0.5, false, true,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.AUTO,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.CPU_X264),
                "fragments");

        assertEquals(1, plan.frameCount());
        assertTrue(plan.exportableAsRenderedVideo());
        Path frame = tempDir.resolve(plan.frames().get(0).imageRelativePath());
        assertTrue(Files.isRegularFile(frame));
        BufferedImage image = ImageIO.read(frame.toFile());
        Color topPixel = new Color(image.getRGB(image.getWidth() / 2, 10));
        assertFalse(topPixel.getGreen() > 120 && topPixel.getRed() < 80, "El playbar verde no debe quemarse en el frame exportado.");
        Color companionGuidePixel = new Color(image.getRGB(48, image.getHeight() / 2));
        Color mapGuidePixel = new Color(image.getRGB((int) (image.getWidth() * 0.72), image.getHeight() / 2));
        Color textGuidePixel = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() - 32));
        assertFalse(companionGuidePixel.getRed() > 140 && companionGuidePixel.getGreen() < 70,
                "El rojo de guia del panel de acompanantes no debe quemarse en el frame.");
        assertFalse(mapGuidePixel.getBlue() > 140 && mapGuidePixel.getRed() < 80,
                "El azul de guia del mapa espacial no debe quemarse en el frame.");
        assertFalse(textGuidePixel.getRed() > 90 && textGuidePixel.getBlue() > 90 && textGuidePixel.getGreen() < 80,
                "El morado de guia del texto no debe quemarse en el frame.");
        Color leftHalfPixel = new Color(image.getRGB(image.getWidth() / 4, image.getHeight() / 2));
        Color rightHalfPixel = new Color(image.getRGB((int) (image.getWidth() * 0.75), image.getHeight() / 2));
        assertTrue(isOrange(leftHalfPixel), "Fragmentos visuales debe usar la mitad izquierda para el fragmento.");
        assertTrue(isWhiteish(rightHalfPixel), "Fragmentos visuales debe usar la mitad derecha para el mapa.");
    }

    @Test
    void noneModeExportsSingleLargeCenteredMapWithoutCompanionPanel() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005.wav");

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP"),
                script(),
                List.of(job()),
                tempDir,
                new SimpleVideoExportSettings(SimpleVideoResolutionPreset.HD_720, 30, 0.5, false, true,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.AUTO,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.CPU_X264),
                "none");

        BufferedImage image = ImageIO.read(tempDir.resolve(plan.frames().get(0).imageRelativePath()).toFile());
        Color centerPixel = new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2));
        Color leftPanelPixel = new Color(image.getRGB(image.getWidth() / 8, image.getHeight() / 2));
        assertTrue(isWhiteish(centerPixel), "Sin acompanante debe centrar el mapa grande en el frame.");
        assertFalse(isOrange(leftPanelPixel), "Sin acompanante no debe dibujar panel ni imagen de fragmento a la izquierda.");
    }

    @Test
    void acceptsRenderedUnitAudioForTheatreSegment() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-UNIT/audio/SEG-005-U001.wav");

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP"),
                script(),
                List.of(job("JOB-UNIT", "SEG-005-U001", "jobs/JOB-UNIT/audio/SEG-005-U001.wav")),
                tempDir,
                SimpleVideoExportSettings.defaults(),
                "fragments");

        assertEquals(1, plan.frameCount());
        assertEquals("jobs/JOB-UNIT/audio/SEG-005-U001.wav", plan.frames().get(0).audioRelativePath());
    }

    @Test
    void keepsFullNarrationTextInTheatreMapFrameMetadata() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005.wav");
        String longText = ("CAPITAN BIGOTE: " + "El viento obedece al piloto que escucha el motor antiguo. ".repeat(8)).strip();

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP"),
                script("SEG-005", "B0005", longText),
                List.of(job()),
                tempDir,
                SimpleVideoExportSettings.defaults(),
                "fragments");

        assertEquals(longText, plan.frames().get(0).narrationPreview());
        assertFalse(plan.frames().get(0).narrationPreview().endsWith("..."));
    }

    @Test
    void theatreMapCaptionUsesFullInterventionTextWhenBlockWasSplitIntoChunks() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005A.wav");
        createAudio("jobs/JOB-001/audio/SEG-005B.wav");
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 2, 2, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-005A", "Texto 5A", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-005A.wav", 1.0, 1, ""),
                new AudioSegmentSnapshot("SEG-005B", "Texto 5B", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-005B.wav", 1.0, 1, "")
        ), Instant.now(), Instant.now());

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP"),
                script(List.of(
                        segment("SEG-005A", "B0005", "NARRADOR: Y asi termino el vuelo del Tornillo Dorado."),
                        segment("SEG-005B", "B0005", "con polvo, aplausos y tres tornillos que nadie se atrevio a preguntar de donde salieron.")
                )),
                List.of(job),
                tempDir,
                SimpleVideoExportSettings.defaults(),
                "fragments");

        String expected = "NARRADOR: Y asi termino el vuelo del Tornillo Dorado. "
                + "con polvo, aplausos y tres tornillos que nadie se atrevio a preguntar de donde salieron.";
        assertEquals(2, plan.frameCount());
        assertEquals(expected, plan.frames().get(0).narrationPreview());
        assertEquals(expected, plan.frames().get(1).narrationPreview());
    }

    @Test
    void theatreMapUsesOnlyExactBlockAudioWhenInterventionPointsToContinuationChunk() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-001/audio/SEG-005A.wav");
        createAudio("jobs/JOB-001/audio/SEG-005B.wav");
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 2, 2, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-005A", "Texto 5A", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-005A.wav", 1.0, 1, ""),
                new AudioSegmentSnapshot("SEG-005B", "Texto 5B", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-005B.wav", 1.0, 1, "")
        ), Instant.now(), Instant.now());

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project("IMG-MAP", "B0006"),
                script(List.of(
                        segment("SEG-005A", "B0005", "NARRADOR: Y asi termino el vuelo del Tornillo Dorado."),
                        segment("SEG-005B", "B0006", "con polvo, aplausos y tres tornillos que nadie se atrevio a preguntar de donde salieron.")
                )),
                List.of(job),
                tempDir,
                SimpleVideoExportSettings.defaults(),
                "fragments");

        assertEquals(1, plan.frameCount());
        assertEquals("SEG-005B", plan.frames().get(0).segmentId());
        assertEquals("con polvo, aplausos y tres tornillos que nadie se atrevio a preguntar de donde salieron.",
                plan.frames().get(0).narrationPreview());
    }

    @Test
    void exportsOnlyInterventionsInsideSelectedSceneScope() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-002/audio/SEG-012.wav");

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                twoSceneProject(),
                script(List.of(
                        segment("SEG-005", "B0005", "NARRADOR: Primera escena."),
                        segment("SEG-012", "B0012", "TENIENTE TORNILLO: Segunda escena."))),
                List.of(job("JOB-002", "SEG-012", "jobs/JOB-002/audio/SEG-012.wav")),
                tempDir,
                SimpleVideoExportSettings.defaults(),
                "none",
                TheatreExportScope.scene("SCN-AIRE"));

        assertEquals(1, plan.frameCount());
        assertEquals("INTERVENCION-2", plan.frames().get(0).title());
        assertEquals("SEG-012", plan.frames().get(0).segmentId());
    }

    @Test
    void mapExportUsesIntermediateFrameOnlyBetweenIncludedAdjacentInterventions() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createImage("assets/fragmentos/inferido.png", Color.GREEN);
        createImage("assets/personajes/capitan.png", Color.BLUE);
        createAudio("jobs/JOB-PAIR/audio/SEG-005.wav");
        createAudio("jobs/JOB-PAIR/audio/SEG-012.wav");
        AudioJobSnapshot job = new AudioJobSnapshot("JOB-PAIR", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 2, 2, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-PAIR", "", "jobs/JOB-PAIR/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-005", "Texto 5", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-PAIR/audio/SEG-005.wav", 2.0, 1, ""),
                new AudioSegmentSnapshot("SEG-012", "Texto 12", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-PAIR/audio/SEG-012.wav", 2.0, 1, "")
        ), Instant.now(), Instant.now());
        DocuPodcastProject base = twoSceneProject()
                .withAsset(new ProjectAssetReference("IMG-INF", ProjectAssetKind.IMAGE, "Inferido",
                        "assets/fragmentos/inferido.png", "image/png", "frame", "", ""));
        DocuPodcastProject project = base.withTheatre(base.theatre().withIntermediateFrames(List.of(
                new TheatreProjectLayer.IntermediateFrame("INTERVENCION-1", "INTERVENCION-2", "IMG-INF", ""))));
        SimpleVideoExportSettings settings = SimpleVideoExportSettings.defaults().withIncludeInferredFrames(true);

        SimpleVideoPlan plan = new BuildTheatreSpatialVideoPlanUseCase().build(
                project,
                script(List.of(
                        segment("SEG-005", "B0005", "NARRADOR: Primera escena."),
                        segment("SEG-012", "B0012", "TENIENTE TORNILLO: Segunda escena."))),
                List.of(job),
                tempDir,
                settings,
                "fragments");

        assertEquals(2, plan.frameCount());
        SimpleVideoFrame first = plan.frames().getFirst();
        assertEquals(2, first.visualParts().size());
        assertEquals(first.frameDurationSeconds() / 2.0, first.visualParts().get(0).durationSeconds(), 0.0001);
        assertEquals(first.frameDurationSeconds() / 2.0, first.visualParts().get(1).durationSeconds(), 0.0001);
        assertEquals("IMG-INF", first.visualParts().get(1).imageAssetId());
        assertTrue(first.visualParts().get(1).imageRelativePath().endsWith("-inferido.png"));
        assertTrue(Files.isRegularFile(tempDir.resolve(first.visualParts().get(1).imageRelativePath())));

        SimpleVideoPlan scoped = new BuildTheatreSpatialVideoPlanUseCase().build(
                project,
                script(List.of(
                        segment("SEG-005", "B0005", "NARRADOR: Primera escena."),
                        segment("SEG-012", "B0012", "TENIENTE TORNILLO: Segunda escena."))),
                List.of(job),
                tempDir,
                settings,
                "fragments",
                TheatreExportScope.scene("SCN-HANGAR"));
        assertEquals(1, scoped.frameCount());
        assertEquals(1, scoped.frames().getFirst().visualParts().size());
    }

    @Test
    void failsWhenSceneHasNoSpatialMapAsset() {
        IOException error = assertThrows(IOException.class, () -> new BuildTheatreSpatialVideoPlanUseCase().build(
                project(""),
                script(),
                List.of(job()),
                tempDir,
                SimpleVideoExportSettings.defaults(),
                "characters"));

        assertTrue(error.getMessage().contains("mapa_espacial"));
    }

    private DocuPodcastProject project(String mapAssetId) throws IOException {
        return project(mapAssetId, "B0005");
    }

    private DocuPodcastProject project(String mapAssetId, String interventionBlockId) throws IOException {
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("IMG-MAP", ProjectAssetKind.IMAGE, "Mapa", "assets/mapas/mapa.png", "image/png", "mapa", "", ""))
                .withAsset(new ProjectAssetReference("IMG-FRAG", ProjectAssetKind.IMAGE, "Fragmento", "assets/fragmentos/uno.png", "image/png", "fragmento", "", ""))
                .withAsset(new ProjectAssetReference("IMG-CHR", ProjectAssetKind.IMAGE, "Capitan", "assets/personajes/capitan.png", "image/png", "personaje", "", ""));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, interventionBlockId)),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-CAPITAN", "CAPITAN BIGOTE", List.of(), "")),
                List.of(),
                List.of(new TheatreProjectLayer.CharacterImage("CHR-CAPITAN", "SCN-HANGAR", "Frontal", "IMG-CHR", "")),
                List.of(new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-FRAG", "")),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "El vuelo", "")),
                List.of(new TheatreProjectLayer.Scene("SCN-HANGAR", "El hangar", "", "ACT-001", mapAssetId)),
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SCN-HANGAR", "CHR-CAPITAN",
                        "fondo centro", "centro derecha", "CAPITAN BIGOTE", Map.of("CAPITAN BIGOTE", "fondo centro"))),
                List.of(),
                List.of());
        return project.withTheatre(theatre);
    }

    private DocuPodcastProject twoSceneProject() {
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("IMG-MAP", ProjectAssetKind.IMAGE, "Mapa", "assets/mapas/mapa.png", "image/png", "mapa", "", ""))
                .withAsset(new ProjectAssetReference("IMG-FRAG", ProjectAssetKind.IMAGE, "Fragmento", "assets/fragmentos/uno.png", "image/png", "fragmento", "", ""))
                .withAsset(new ProjectAssetReference("IMG-CHR", ProjectAssetKind.IMAGE, "Capitan", "assets/personajes/capitan.png", "image/png", "personaje", "", ""));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(
                        TheatreProjectLayer.Intervencion.ofSequence(1, "B0005"),
                        TheatreProjectLayer.Intervencion.ofSequence(2, "B0012")),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-CAPITAN", "CAPITAN BIGOTE", List.of(), "")),
                List.of(),
                List.of(new TheatreProjectLayer.CharacterImage("CHR-CAPITAN", "", "Frontal", "IMG-CHR", "")),
                List.of(
                        new TheatreProjectLayer.IntervencionVisual("INTERVENCION-1", "IMG-FRAG", ""),
                        new TheatreProjectLayer.IntervencionVisual("INTERVENCION-2", "IMG-FRAG", "")),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "El vuelo", "")),
                List.of(
                        new TheatreProjectLayer.Scene("SCN-HANGAR", "El hangar", "", "ACT-001", "IMG-MAP"),
                        new TheatreProjectLayer.Scene("SCN-AIRE", "En el aire", "", "ACT-001", "IMG-MAP")),
                List.of(),
                List.of(),
                List.of(
                        new TheatreProjectLayer.TextActionPlacement("INTERVENCION-1", "SCN-HANGAR", "CHR-CAPITAN",
                                "fondo centro", "centro derecha", "CAPITAN BIGOTE", Map.of("CAPITAN BIGOTE", "fondo centro")),
                        new TheatreProjectLayer.TextActionPlacement("INTERVENCION-2", "SCN-AIRE", "CHR-CAPITAN",
                                "centro derecha", "centro izquierda", "CAPITAN BIGOTE", Map.of("CAPITAN BIGOTE", "centro derecha"))),
                List.of(),
                List.of());
        return project.withTheatre(theatre);
    }

    private NarrationScriptDocument script() {
        return script("SEG-005", "B0005", "CAPITAN BIGOTE: Revise el combustible.");
    }

    private NarrationScriptDocument script(String segmentId, String blockId, String text) {
        return script(List.of(segment(segmentId, blockId, text)));
    }

    private NarrationScriptDocument script(List<NarrationSegment> segments) {
        return NarrationScriptDocument.create("Demo", "es", "source.docx", segments);
    }

    private NarrationSegment segment(String segmentId, String blockId, String text) {
        return NarrationSegment.of(segmentId, NarrationSegmentType.PARAGRAPH,
                "Texto " + blockId.replaceAll("\\D+", ""), text, List.of(blockId));
    }

    private AudioJobSnapshot job() {
        return job("JOB-001", "SEG-005", "jobs/JOB-001/audio/SEG-005.wav");
    }

    private AudioJobSnapshot job(String jobId, String segmentId, String audioPath) {
        return new AudioJobSnapshot(jobId, "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0,
                "OK", "jobs/" + jobId, "", "jobs/" + jobId + "/audio-manifest.json", List.of(
                new AudioSegmentSnapshot(segmentId, "Texto 5", AudioSegmentStatus.COMPLETED,
                        audioPath, 2.0, 1, "")
        ), Instant.now(), Instant.now());
    }

    private void createImage(String relativePath, Color color) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", file.toFile());
    }

    private void createAudio(String relativePath) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "fake wav for plan resolution");
    }

    private static boolean isOrange(Color color) {
        return color.getRed() > 180 && color.getGreen() > 80 && color.getGreen() < 230 && color.getBlue() < 80;
    }

    private static boolean isWhiteish(Color color) {
        return color.getRed() > 180 && color.getGreen() > 180 && color.getBlue() > 180;
    }
}
