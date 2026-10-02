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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildTheatreWorkVideoPlanUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void buildsComposedTheatreWorkFramesFromDialogOptions() throws Exception {
        createImage("assets/mapas/mapa.png", Color.WHITE);
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createAudio("jobs/JOB-001/audio/SEG-005.wav");

        SimpleVideoPlan plan = new BuildTheatreWorkVideoPlanUseCase().build(
                project("IMG-MAP"),
                script(),
                List.of(job()),
                tempDir,
                new SimpleVideoExportSettings(SimpleVideoResolutionPreset.HD_720, 30, 0.5, false, true,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy.AUTO,
                        com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy.CPU_X264),
                new TheatreWorkVideoOptions(true, true, "Arial", 36, "#111827",
                        TheatreWorkVideoOptions.TextPosition.BOTTOM,
                        TheatreWorkVideoOptions.TextEffect.SHADOW,
                        TheatreWorkVideoOptions.FrameLayout.IMAGE_WITH_SPATIAL_MAP,
                        true, true, true, "#FFFFFF"));

        assertEquals(1, plan.frameCount());
        assertTrue(plan.exportableAsRenderedVideo());
        Path frame = tempDir.resolve(plan.frames().get(0).imageRelativePath());
        assertTrue(Files.isRegularFile(frame));
        BufferedImage image = ImageIO.read(frame.toFile());
        assertTrue(isOrange(new Color(image.getRGB(image.getWidth() / 3, image.getHeight() / 2))),
                "La obra debe usar la imagen teatral del fragmento.");
        assertTrue(isWhiteish(new Color(image.getRGB((int) (image.getWidth() * 0.86), image.getHeight() / 2))),
                "La obra debe dibujar el mapa espacial lateral cuando se solicita.");
        assertEquals("CAPITAN BIGOTE: Revise el combustible.", plan.frames().get(0).narrationPreview());
        assertEquals("IMG-FRAG", plan.frames().get(0).imageAssetId());
        assertEquals(1, plan.frames().get(0).characterLabels().size());
    }

    @Test
    void mapOptionDoesNotBlockTheatreWorkWhenSceneHasNoSpatialMap() throws Exception {
        createImage("assets/fragmentos/uno.png", Color.ORANGE);
        createAudio("jobs/JOB-001/audio/SEG-005.wav");

        SimpleVideoPlan plan = new BuildTheatreWorkVideoPlanUseCase().build(
                project(""),
                script(),
                List.of(job()),
                tempDir,
                SimpleVideoExportSettings.defaults(),
                new TheatreWorkVideoOptions(true, true, "Arial", 36, "#111827",
                        TheatreWorkVideoOptions.TextPosition.CENTER,
                        TheatreWorkVideoOptions.TextEffect.NONE,
                        TheatreWorkVideoOptions.FrameLayout.IMAGE_WITH_SPATIAL_MAP,
                        true, true, true, "#FFFFFF"));

        assertEquals(1, plan.frameCount());
        assertTrue(plan.exportableAsRenderedVideo());
        assertTrue(Files.isRegularFile(tempDir.resolve(plan.frames().get(0).imageRelativePath())));
    }

    private DocuPodcastProject project(String mapAssetId) {
        DocuPodcastProject project = DocuPodcastProject.empty("Demo")
                .withAsset(new ProjectAssetReference("IMG-FRAG", ProjectAssetKind.IMAGE, "Fragmento",
                        "assets/fragmentos/uno.png", "image/png", "fragmento", "", ""));
        if (!mapAssetId.isBlank()) {
            project = project.withAsset(new ProjectAssetReference("IMG-MAP", ProjectAssetKind.IMAGE, "Mapa",
                    "assets/mapas/mapa.png", "image/png", "mapa", "", ""));
        }
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(TheatreProjectLayer.Intervencion.ofSequence(1, "B0005")),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-CAPITAN", "CAPITAN BIGOTE", List.of(), "")),
                List.of(),
                List.of(),
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

    private NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-005", NarrationSegmentType.PARAGRAPH,
                        "Texto 5", "CAPITAN BIGOTE: Revise el combustible.", List.of("B0005"))));
    }

    private AudioJobSnapshot job() {
        return new AudioJobSnapshot("JOB-001", "Demo", AudioJobState.COMPLETED,
                AudioGenerationStage.EXPORT_READY, 1, 1, 0, 1.0, "", "", 0,
                "OK", "jobs/JOB-001", "", "jobs/JOB-001/audio-manifest.json", List.of(
                new AudioSegmentSnapshot("SEG-005", "Texto 5", AudioSegmentStatus.COMPLETED,
                        "jobs/JOB-001/audio/SEG-005.wav", 2.0, 1, "")
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
