package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.compatibility.media.LegacyEnginePresetMapper;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageSuperResolutionCoordinator;
import com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.services.StoryboardApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ImageAssetImportResult;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Builds a neutral 16:9 request and imports the resulting project asset. */
public final class NarrativeImageGenerationWorkflow {
    private final StoryboardApplicationServices storyboard;
    private final MediaCapabilityService media;

    public NarrativeImageGenerationWorkflow(StoryboardApplicationServices storyboard, MediaCapabilityService media) {
        this.storyboard = Objects.requireNonNull(storyboard, "storyboard services");
        this.media = Objects.requireNonNull(media, "media capabilities");
    }

    public Result generate(ProjectSession session, NarrationSegment segment, OperationalSettings settings,
                           Consumer<String> progress) throws IOException {
        if (session == null) throw new IOException("Abre un proyecto antes de generar imagen narrativa.");
        if (segment == null || !segment.narratable()) {
            throw new IOException("El fragmento narrativo no tiene texto suficiente para generar imagen.");
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de generar imágenes narrativas."));
        OperationalSettings resolved = settings == null ? OperationalSettings.defaults() : settings;
        ImageGenerationSettings image = resolved.imageGeneration();
        Path outputDirectory = projectFile.toAbsolutePath().normalize().getParent()
                .resolve("generated/narrativa-ia").resolve(safe(segment.id()));
        VisualResolutionProfile profile = ImageSuperResolutionCoordinator.resolveGenerationProfile(
                session.project(), resolved.imageSuperResolution());
        var delivery = profile.deliveryDimensions(16, 9);
        var working = profile.workingDimensions(16, 9);
        ImageGenerationRequest request = new ImageGenerationRequest(
                positivePrompt(segment),
                "low quality, blurry, deformed hands, duplicated faces, unreadable text, watermark",
                working.width(), working.height(), List.of(), outputDirectory,
                "narrativa-" + safe(segment.id()),
                Map.of("deliveryWidth", Integer.toString(delivery.width()),
                        "deliveryHeight", Integer.toString(delivery.height())),
                LegacyEnginePresetMapper.image(image.preset()), List.of(),
                Math.abs(java.util.Objects.hash(segment.id(), segment.narrationText())), 1);
        ExecutionPolicy policy = new ExecutionPolicy(Duration.ofSeconds(image.timeoutSeconds()), image.maxAttempts());
        ExecutionContext context = new ExecutionContext("narrative-image-" + segment.id(), CancellationToken.NONE,
                (stage, amount, message) -> progress(progress, message), policy, ResourceLease.NONE);
        progress(progress, "Generando imagen principal 16:9 para " + segment.id() + ".");
        ImageGenerationResult generated;
        try {
            generated = media.generateImage(SelectedMediaEngines.from(resolved).image(), request, context);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Generación de imagen interrumpida.", ex);
        }
        Path rawOutput = generated.images().stream().findFirst()
                .orElseThrow(() -> new IOException("El motor no produjo una imagen."));
        ImageSuperResolutionCoordinator.Result postprocessed =
                new ImageSuperResolutionCoordinator(media).processGenerated(
                        session.project(), resolved.imageSuperResolution(), rawOutput,
                        outputDirectory, "narrativa-" + safe(segment.id()) + "-upscaled",
                        16, 9, request.prompt(), context);
        if (!postprocessed.warning().isBlank()) progress(progress, postprocessed.warning());
        Path output = postprocessed.output();
        ImageAssetImportResult imported = storyboard.importImageAsset()
                .importImage(session.project(), projectFile, output);
        session.replaceProject(imported.project(), true);
        return new Result(imported, output, "Imagen narrativa generada e importada para " + segment.id() + ".");
    }

    private static String positivePrompt(NarrationSegment segment) {
        String source = segment.metadata() == null ? "" : List.of(
                "visualPrompt", "visual_prompt", "imagePrompt", "image_prompt", "prompt", "visual").stream()
                .map(segment.metadata()::get).filter(value -> value != null && !value.isBlank()).findFirst().orElse("");
        if (source.isBlank()) source = segment.narrationText();
        return "cinematic documentary narrative frame, 16:9 composition, editorial still, "
                + "clear subject, natural lighting, no text overlays, scene inspired by: " + source;
    }

    private static String safe(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-").replaceAll("^-+|-+$", "").replaceAll("-+", "-");
        return normalized.isBlank() ? "fragmento" : normalized;
    }

    private static void progress(Consumer<String> progress, String message) {
        if (progress != null && message != null && !message.isBlank()) progress.accept(message);
    }

    public record Result(ImageAssetImportResult importResult, Path outputPath, String message) {
        public Result { message = message == null ? "" : message.strip(); }
    }
}
