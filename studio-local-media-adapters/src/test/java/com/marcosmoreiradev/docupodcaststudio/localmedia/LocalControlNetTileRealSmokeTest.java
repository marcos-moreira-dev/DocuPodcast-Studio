package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageRefinementRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageSuperResolutionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Opt-in hardware smoke:
 * -Ddocupodcast.realControlNetSmoke=true
 * -Ddocupodcast.realControlNetInput=...
 * -Ddocupodcast.realControlNetOutput=...
 */
class LocalControlNetTileRealSmokeTest {
    @Test
    void gpuFirstUpscaleThenConservativeTileRefinementKeepsTheOriginal() throws Exception {
        assumeTrue(Boolean.getBoolean("docupodcast.realControlNetSmoke"));
        Path root = Path.of(System.getProperty("docupodcast.repositoryRoot", "."))
                .toAbsolutePath().normalize();
        Path input = Path.of(System.getProperty("docupodcast.realControlNetInput",
                "C:/Users/MARCOS MOREIRA/Desktop/docupodcast-imagen-prueba-prueba-imagen.png"));
        Path output = Path.of(System.getProperty("docupodcast.realControlNetOutput",
                "C:/Users/MARCOS MOREIRA/Desktop/docupodcast-imagen-prueba-prueba-imagen-4k-mejorada.png"));
        assumeTrue(Files.isRegularFile(input), "Falta la imagen de entrada del smoke.");
        String originalHash = ManagedDownloadPreflightInspector.sha256(input);

        var platform = LocalMediaAdapters.create(LocalMediaLayout.development(root));
        var lifecycle = platform.administration().require(ComfyUiImageEngine.ID);
        ExecutionContext context = new ExecutionContext(
                "real-controlnet-tile-smoke",
                CancellationToken.NONE,
                ProgressSink.NONE,
                new ExecutionPolicy(Duration.ofHours(3), 1),
                ResourceLease.NONE);
        try {
            lifecycle.execute(new EngineActionRequest(
                    ComfyUiImageEngine.ID,
                    EngineActionId.START,
                    Map.of("memoryProfile", "SAFE_LOW_VRAM")),
                    context);

            Path staging = root.resolve("diagnostics/controlnet-tile-real-smoke");
            var upscaled = platform.imageSuperResolutionEngines()
                    .require(ComfyUiSuperResolutionEngine.ID)
                    .upscale(new ImageSuperResolutionRequest(
                            input, staging, "input-4k",
                            3840, 2160,
                            ImageSuperResolutionRequest.DEFAULT_MODEL,
                            false,
                            Map.of("smoke", "true")),
                            context);
            var refined = platform.imageRefinementEngines()
                    .require(ComfyUiTileRefinementEngine.ID)
                    .refine(new ImageRefinementRequest(
                            upscaled.image(),
                            output.toAbsolutePath().normalize().getParent(),
                            stripPng(output.getFileName().toString()),
                            "illustrated bicycle store, preserve composition, identity, geometry and lighting",
                            ImageRefinementRequest.CONSERVATIVE,
                            424242L,
                            Map.of("tileSize", "512", "smoke", "true")),
                            context);

            assertTrue(refined.refined());
            assertEquals(output.toAbsolutePath().normalize(), refined.image().toAbsolutePath().normalize());
            var image = ImageIO.read(output.toFile());
            assertEquals(3840, image.getWidth());
            assertEquals(2160, image.getHeight());
            assertEquals(originalHash, ManagedDownloadPreflightInspector.sha256(input));
        } finally {
            lifecycle.execute(new EngineActionRequest(
                    ComfyUiImageEngine.ID, EngineActionId.STOP, Map.of()), context);
        }
    }

    private static String stripPng(String filename) {
        return filename.toLowerCase(java.util.Locale.ROOT).endsWith(".png")
                ? filename.substring(0, filename.length() - 4)
                : filename;
    }
}
