package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupportPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.AiModelResourceGuard;
import com.marcosmoreiradev.docupodcaststudio.application.process.AiModelResourceGuard.AiModelResourceKind;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationTaskKind;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ImageAssetImportResult;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineRequest;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineResult;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Generates and imports one 16:9 main image for a Video narrativo fragment. */
public final class NarrativeImageGenerationWorkflow {
    private final ApplicationServices services;
    private final ComfyUiVisualEngineClient visualEngineClient;
    private final AiModelResourceGuard resourceGuard;

    public NarrativeImageGenerationWorkflow(ApplicationServices services) {
        this(services, visualClientFrom(services));
    }

    public NarrativeImageGenerationWorkflow(ApplicationServices services, ComfyUiVisualEngineClient visualEngineClient) {
        this.services = Objects.requireNonNull(services, "services");
        this.visualEngineClient = Objects.requireNonNull(visualEngineClient, "visualEngineClient");
        this.resourceGuard = AiModelResourceGuard.global();
    }

    public Result generate(ProjectSession session,
                           NarrationSegment segment,
                           OperationalSettings settings,
                           Consumer<String> progress) throws IOException {
        if (session == null) {
            throw new IOException("Abre un proyecto antes de generar imagen narrativa.");
        }
        if (segment == null || !segment.narratable()) {
            throw new IOException("El fragmento narrativo no tiene texto suficiente para generar imagen.");
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de generar imagenes narrativas."));
        OperationalSettings resolvedSettings = settings == null ? OperationalSettings.defaults() : settings;
        ImageGenerationSettings image = resolvedSettings.imageGeneration();
        ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forPresetId(image.preset());
        if (!support.builtInWorkflowAvailable()) {
            throw new IOException(support.userMessage() + "\n" + support.diagnostic());
        }
        Path outputDirectory = projectFile.toAbsolutePath().normalize().getParent()
                .resolve("generated/narrativa-ia")
                .resolve(safe(segment.id()));
        ImageEnhancementOutputProfile profile = ImageEnhancementOutputProfile.FHD_1080;
        String checkpoint = image.modelName().isBlank() ? support.checkpointName() : image.modelName();
        VisualEngineRequest request = new VisualEngineRequest(
                positivePrompt(segment),
                "low quality, blurry, deformed hands, duplicated faces, unreadable text, watermark",
                checkpoint,
                support.defaultSteps(),
                support.defaultCfg(),
                support.defaultBatchSize(),
                profile.width(),
                profile.height(),
                outputDirectory,
                "narrativa-" + safe(segment.id()));

        progress(progress, "Generando imagen principal 16:9 para " + segment.id() + ".");
        VisualEngineResult result;
        try (AiModelResourceGuard.Lease ignored = resourceGuard.acquire(AiModelResourceKind.IMAGE,
                "video narrativo " + segment.id())) {
            result = visualEngineClient.generate(image.baseUrl(),
                    java.time.Duration.ofSeconds(image.timeoutSeconds()),
                    request,
                    GenerationAttemptPolicy.fromSettings(resolvedSettings),
                    GenerationTaskKind.IMAGE_CANDIDATE,
                    progress);
        }
        progress(progress, "Importando PNG al proyecto.");
        ImageAssetImportResult imported = services.storyboard().importImageAsset()
                .importImage(session.project(), projectFile, result.outputPath());
        session.replaceProject(imported.project(), true);
        return new Result(imported, result.outputPath(),
                "Imagen IA narrativa generada e importada para " + segment.id() + ".");
    }

    private static String positivePrompt(NarrationSegment segment) {
        String visualPrompt = metadataPrompt(segment.metadata());
        String source = visualPrompt.isBlank() ? segment.narrationText() : visualPrompt;
        return "cinematic documentary narrative frame, 16:9 composition, editorial still, "
                + "clear subject, natural lighting, no text overlays, scene inspired by: "
                + source;
    }

    private static String metadataPrompt(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        for (String key : ListKeys.VISUAL_PROMPT_KEYS) {
            String value = metadata.get(key);
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "";
    }

    private static String safe(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
        return normalized.isBlank() ? "fragmento" : normalized;
    }

    private static void progress(Consumer<String> progress, String message) {
        if (progress != null) {
            progress.accept(message);
        }
    }

    private static ComfyUiVisualEngineClient visualClientFrom(ApplicationServices services) {
        if (services == null || services.visual() == null) {
            return new ComfyUiVisualEngineClient();
        }
        return services.visual().comfyUiVisualEngineClient();
    }

    public record Result(ImageAssetImportResult importResult, Path outputPath, String message) {
        public Result {
            message = message == null ? "" : message.strip();
        }
    }

    private static final class ListKeys {
        private static final java.util.List<String> VISUAL_PROMPT_KEYS = java.util.List.of(
                "visualPrompt", "visual_prompt", "imagePrompt", "image_prompt", "prompt", "visual");

        private ListKeys() {
        }
    }
}
