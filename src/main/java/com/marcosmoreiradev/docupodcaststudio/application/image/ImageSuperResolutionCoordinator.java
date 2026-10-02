package com.marcosmoreiradev.docupodcaststudio.application.image;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageSuperResolutionSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectVisualProcessingSettings;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageSuperResolutionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageSuperResolutionResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageRefinementRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageRefinementResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EnginePresetId;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Cross-engine postprocessor. It receives an already rendered image and never invokes image generation.
 */
public final class ImageSuperResolutionCoordinator {
    private final MediaCapabilityService media;

    public ImageSuperResolutionCoordinator(MediaCapabilityService media) {
        this.media = Objects.requireNonNull(media, "media capabilities");
    }

    public Result processGenerated(
            DocuPodcastProject project,
            ImageSuperResolutionSettings global,
            Path source,
            Path outputDirectory,
            String filenamePrefix,
            int widthRatio,
            int heightRatio,
            ExecutionContext context) {
        return processGenerated(project, global, source, outputDirectory, filenamePrefix,
                widthRatio, heightRatio, "", context);
    }

    public Result processGenerated(
            DocuPodcastProject project,
            ImageSuperResolutionSettings global,
            Path source,
            Path outputDirectory,
            String filenamePrefix,
            int widthRatio,
            int heightRatio,
            String prompt,
            ExecutionContext context) {
        ImageSuperResolutionSettings defaults = global == null
                ? ImageSuperResolutionSettings.defaults() : global;
        ProjectVisualProcessingSettings overrides = project == null
                ? ProjectVisualProcessingSettings.inherited() : project.visualProcessing();
        boolean enabled = overrides.upscaleEnabled() == null
                ? defaults.enabled() : overrides.upscaleEnabled();
        if (!enabled) return Result.unchanged(source);

        VisualResolutionProfile targetProfile = VisualResolutionProfile.from(
                overrides.upscaleTargetProfile().isBlank()
                        ? defaults.targetProfile() : overrides.upscaleTargetProfile(),
                defaults.targetResolution());
        String model = overrides.upscaleModelName().isBlank()
                ? defaults.modelName() : overrides.upscaleModelName();
        VisualResolutionProfile.Dimensions target =
                targetProfile.deliveryDimensions(widthRatio, heightRatio);
        try {
            BufferedImage input = ImageIO.read(source.toFile());
            if (input == null) throw new IOException("El PNG generado no puede decodificarse.");
            if (!target.isStrictlyLargerThan(input.getWidth(), input.getHeight())) {
                return Result.warning(source,
                        "Se conservó el PNG original: el destino configurado no es superior en ambos ejes.");
            }
            ImageSuperResolutionResult result = media.upscaleImage(null,
                    new ImageSuperResolutionRequest(source, outputDirectory, filenamePrefix,
                            target.width(), target.height(), model, false,
                            Map.of("sourceKind", "generated-frame")),
                    context);
            if (!Files.isRegularFile(result.image())) {
                return Result.warning(source,
                        "Se conservó el PNG original: Real-ESRGAN no produjo un archivo verificable.");
            }
            boolean refine = overrides.refineAfterUpscale() == null
                    ? defaults.refinementEnabled() : overrides.refineAfterUpscale();
            if (!refine) {
                Files.deleteIfExists(source);
                return new Result(result.image(), true, false, "");
            }
            String preset = overrides.refinementPreset().isBlank()
                    ? defaults.refinementPreset() : overrides.refinementPreset();
            String engine = overrides.refinementEngineId().isBlank()
                    ? defaults.refinementEngineId() : overrides.refinementEngineId();
            try {
                ImageRefinementResult refined = media.refineImage(new EngineId(engine),
                        new ImageRefinementRequest(
                                result.image(),
                                outputDirectory,
                                filenamePrefix + "-refined",
                                prompt,
                                new EnginePresetId(preset),
                                Math.abs(Objects.hash(filenamePrefix, prompt)),
                                Map.of("sourceKind", "upscaled-generated-frame")),
                        context);
                if (!refined.refined() || !Files.isRegularFile(refined.image())) {
                    throw new IOException("ControlNet Tile no produjo un PNG refinado verificable.");
                }
                BufferedImage verified = ImageIO.read(refined.image().toFile());
                if (verified == null || verified.getWidth() != target.width()
                        || verified.getHeight() != target.height()) {
                    throw new IOException("El refinado no conservó las dimensiones reescaladas.");
                }
                Files.deleteIfExists(source);
                Files.deleteIfExists(result.image());
                return new Result(refined.image(), true, true, "");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                Files.deleteIfExists(source);
                return Result.refinementWarning(result.image(),
                        "La mejora de composición fue cancelada; se conservó la imagen reescalada.");
            } catch (IOException | RuntimeException refinementFailure) {
                Files.deleteIfExists(source);
                return Result.refinementWarning(result.image(),
                        "No se pudo mejorar la composición; se conservó la imagen reescalada. "
                                + rootMessage(refinementFailure));
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Result.warning(source,
                    "Se conservó el PNG original: la superresolución fue cancelada.");
        } catch (IOException | RuntimeException failure) {
            return Result.warning(source, "Se conservó el PNG original: falló la superresolución IA. "
                    + rootMessage(failure));
        }
    }

    public static VisualResolutionProfile resolveGenerationProfile(
            DocuPodcastProject project, ImageSuperResolutionSettings global) {
        ImageSuperResolutionSettings defaults = global == null
                ? ImageSuperResolutionSettings.defaults() : global;
        String override = project == null ? "" : project.visualProcessing().generationProfile();
        return VisualResolutionProfile.from(
                override.isBlank() ? defaults.generationProfile() : override,
                defaults.generationResolution());
    }

    private static String rootMessage(Throwable failure) {
        Throwable current = failure;
        while (current != null && current.getCause() != null) current = current.getCause();
        return current == null || current.getMessage() == null ? "Error desconocido." : current.getMessage();
    }

    public record Result(Path output, boolean applied, boolean refined, String warning) {
        public Result {
            if (output == null) throw new IllegalArgumentException("output is required");
            warning = warning == null ? "" : warning.strip();
        }

        static Result unchanged(Path source) {
            return new Result(source, false, false, "");
        }

        static Result warning(Path source, String warning) {
            return new Result(source, false, false, warning);
        }

        static Result refinementWarning(Path upscaled, String warning) {
            return new Result(upscaled, true, false, warning);
        }
    }
}
