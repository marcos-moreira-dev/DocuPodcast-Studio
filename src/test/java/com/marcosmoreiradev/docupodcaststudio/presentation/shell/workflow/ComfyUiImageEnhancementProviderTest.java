package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageAspectStrategy;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementRequest;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComfyUiImageEnhancementProviderTest {
    @TempDir
    Path temp;

    @Test
    void enhancesSquareImageTo1080pAndWritesManifest() throws Exception {
        Path input = temp.resolve("source.png");
        BufferedImage source = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        g.setColor(new Color(120, 80, 40));
        g.fillRect(0, 0, 500, 500);
        g.setColor(Color.WHITE);
        g.fillOval(150, 90, 200, 260);
        g.dispose();
        ImageIO.write(source, "png", input.toFile());

        ImageEnhancementRequest request = new ImageEnhancementRequest(
                "test-job",
                input,
                temp,
                ImageEnhancementOutputProfile.FHD_1080,
                ImageAspectStrategy.PRESERVE_WITH_PADDING,
                ImageEnhancementOutputProfile.FHD_1080.pipelineProfile(),
                "comfyui-local",
                ImageEnhancementOutputProfile.FHD_1080.workflowId(),
                "prueba teatral",
                24,
                0.35,
                768,
                96,
                true,
                "capitan-bigote-lora");

        ImageEnhancementResult result = new ComfyUiImageEnhancementProvider().enhance(request);

        assertTrue(result.success(), result.message());
        assertTrue(Files.isRegularFile(result.finalImage()));
        assertTrue(Files.isRegularFile(result.intermediateImage()));
        assertTrue(Files.isRegularFile(result.manifest()));
        BufferedImage finalImage = ImageIO.read(result.finalImage().toFile());
        assertNotNull(finalImage);
        assertEquals(1920, finalImage.getWidth());
        assertEquals(1080, finalImage.getHeight());
        String manifest = Files.readString(result.manifest());
        assertTrue(manifest.contains("\"targetWidth\": 1920"));
        assertTrue(manifest.contains("\"targetHeight\": 1080"));
        assertTrue(manifest.contains("\"aspectStrategy\": \"PRESERVE_WITH_PADDING\""));
        assertTrue(manifest.contains("\"loraName\": \"capitan-bigote-lora\""));
        assertTrue(manifest.contains("\"finalImage\""));
    }

    @Test
    void outpaintToTargetRequiresRealComfyUiWorkflow() throws Exception {
        Path input = temp.resolve("source.png");
        BufferedImage source = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        g.setColor(new Color(30, 40, 80));
        g.fillRect(0, 0, 500, 500);
        g.dispose();
        ImageIO.write(source, "png", input.toFile());

        ImageEnhancementRequest request = new ImageEnhancementRequest(
                "outpaint-job",
                input,
                temp,
                ImageEnhancementOutputProfile.FHD_1080,
                ImageAspectStrategy.OUTPAINT_TO_TARGET,
                ImageEnhancementOutputProfile.FHD_1080.pipelineProfile(),
                "comfyui-local",
                ImageEnhancementOutputProfile.FHD_1080.workflowId(),
                "prueba teatral",
                24,
                0.35,
                768,
                96,
                true,
                "capitan-bigote-lora");

        ImageEnhancementResult result = new ComfyUiImageEnhancementProvider().enhance(request);

        assertTrue(!result.success());
        assertTrue(result.message().contains("workflow ComfyUI real"));
    }
}
