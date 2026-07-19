package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupportPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxLicenseAcceptanceStore;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxMemoryPreflight;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxModelBundle;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.process.AiModelResourceGuard;
import com.marcosmoreiradev.docupodcaststudio.application.process.AiModelResourceGuard.AiModelResourceKind;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationTaskKind;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ComfyUiConnectionSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreVisualGenerationContextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedFrameCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedImageCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageContextAsset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatrePrimaryVisualReference;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreVisualGenerationContext;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiWorkflowSpec;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningReference;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningRole;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineRequest;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineResult;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Coordinates local ComfyUI generation and reviewable theatre image candidates. */
public final class TheatreImageGenerationWorkflow {
    private final ApplicationServices services;
    private final TheatreGenerationUnitPlanner planner = new TheatreGenerationUnitPlanner();
    private final BuildTheatreVisualGenerationContextUseCase contextBuilder = new BuildTheatreVisualGenerationContextUseCase();
    private final ComfyUiVisualEngineClient visualEngineClient;
    private final AiModelResourceGuard resourceGuard;

    public TheatreImageGenerationWorkflow(ApplicationServices services) {
        this(services, visualClientFrom(services));
    }

    public TheatreImageGenerationWorkflow(ApplicationServices services, ComfyUiVisualEngineClient visualEngineClient) {
        this.services = Objects.requireNonNull(services, "services");
        this.visualEngineClient = Objects.requireNonNull(visualEngineClient, "visualEngineClient");
        this.resourceGuard = AiModelResourceGuard.global();
    }

    public List<TheatreImageGenerationUnit> queue(ProjectSession session, com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument script, TheatreContextExportScope scope) {
        return planner.units(session, script, scope);
    }

    public ComfyUiVisualEngineClient.ConnectionResult testConnection(ComfyUiConnectionSettings settings) {
        return visualEngineClient.test(settings.baseUrl(), settings.timeout());
    }

    public List<TheatreImageContextAsset> contextAssets(ProjectSession session, TheatreImageGenerationUnit unit) {
        return generationContext(session, unit).allAssets();
    }

    public TheatreVisualGenerationContext generationContext(ProjectSession session, TheatreImageGenerationUnit unit) {
        if (session == null || unit == null) {
            return new TheatreVisualGenerationContext(unit, List.of(), List.of(), List.of(), null, null, null);
        }
        Path root = session.projectFile().map(path -> path.toAbsolutePath().normalize().getParent()).orElse(null);
        return contextBuilder.execute(session.project(), session.storyboard().orElse(null), root, unit);
    }

    public TheatreGeneratedImageCandidate generate(ProjectSession session, TheatreImageGenerationUnit unit,
                                                   ComfyUiConnectionSettings settings, TheatreImageGenerationPreset preset,
                                                   boolean assignToIntervention) throws IOException {
        return generate(session, unit, settings, preset, ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9, assignToIntervention, null);
    }

    public TheatreGeneratedImageCandidate generate(ProjectSession session, TheatreImageGenerationUnit unit,
                                                   ComfyUiConnectionSettings settings,
                                                   TheatreImageGenerationPreset preset,
                                                   ImageEnhancementOutputProfile outputProfile,
                                                   boolean assignToIntervention) throws IOException {
        return generate(session, unit, settings, preset, outputProfile, TheatreImageAspectRatio.WIDE_16_9,
                assignToIntervention, null);
    }

    public TheatreGeneratedImageCandidate generate(ProjectSession session, TheatreImageGenerationUnit unit,
                                                   ComfyUiConnectionSettings settings,
                                                   TheatreImageGenerationPreset preset,
                                                   ImageEnhancementOutputProfile outputProfile,
                                                   TheatreImageAspectRatio aspectRatio,
                                                   boolean assignToIntervention,
                                                   Consumer<String> progress) throws IOException {
        outputProfile = outputProfile == null ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        aspectRatio = aspectRatio == null ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio;
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException("Guarda el proyecto antes de generar imagenes teatrales."));
        Path outputRoot = settings.outputDirectory() == null
                ? projectFile.toAbsolutePath().normalize().getParent().resolve("generated/teatro-ia")
                : settings.outputDirectory();
        progress(progress, "En cola.");
        Path output;
        try (AiModelResourceGuard.Lease ignored = resourceGuard.acquire(AiModelResourceKind.IMAGE, unit.interventionId())) {
            Path outputDirectory = outputRoot.resolve(safe(unit.sceneId()))
                    .resolve(safe(unit.interventionId()))
                    .resolve(safe(unit.segmentId()));
            VisualEngineRequest request = visualEngineRequest(session, unit, preset, outputProfile, aspectRatio,
                    outputDirectory, safe(unit.interventionId()) + "-" + safe(unit.segmentId()) + "-" + aspectRatio.workflowId());
            ComfyUiWorkflowSpec workflow = workflowSpec(preset, request);
            VisualEngineResult result = visualEngineClient.generate(settings.baseUrl(), fluxTimeout(settings, preset), request, workflow,
                    attemptPolicy(), GenerationTaskKind.IMAGE_CANDIDATE, progress);
            output = result.outputPath();
        }
        var imported = services.storyboard().importImageAsset().importImage(session.project(), projectFile, output);
        session.replaceProject(imported.project(), true);
        TheatreGeneratedImageCandidate candidate = new TheatreGeneratedImageCandidate(unit.interventionId(), unit.sceneId(), unit.interventionId(),
                unit.segmentId(), imported.imageAsset().id(), output, false);
        return assignToIntervention ? approve(session, candidate) : candidate;
    }

    public TheatreGeneratedImageCandidate generateTransition(ProjectSession session,
                                                             TheatreImageGenerationUnit unit,
                                                             String previousAssetId,
                                                             Path previousFrame,
                                                             String nextAssetId,
                                                             Path nextFrame,
                                                             ComfyUiConnectionSettings settings,
                                                             TheatreImageGenerationPreset preset,
                                                             ImageEnhancementOutputProfile outputProfile,
                                                             TheatreImageAspectRatio aspectRatio,
                                                             Consumer<String> progress) throws IOException {
        outputProfile = outputProfile == null ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        aspectRatio = aspectRatio == null ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio;
        Path previous = previousFrame == null ? null : previousFrame.toAbsolutePath().normalize();
        Path next = nextFrame == null ? null : nextFrame.toAbsolutePath().normalize();
        if (previous == null || !java.nio.file.Files.isRegularFile(previous)
                || next == null || !java.nio.file.Files.isRegularFile(next)) {
            throw new IOException("El frame intermedio requiere los PNG principal anterior y siguiente.");
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de generar imagenes teatrales."));
        Path outputRoot = settings.outputDirectory() == null
                ? projectFile.toAbsolutePath().normalize().getParent().resolve("generated/teatro-ia")
                : settings.outputDirectory();
        progress(progress, "En cola.");
        Path outputDirectory = outputRoot.resolve(safe(unit.sceneId()))
                .resolve(safe(unit.interventionId()))
                .resolve(safe(unit.segmentId()));
        VisualEngineRequest request = visualEngineRequest(unit, preset, outputProfile, aspectRatio,
                outputDirectory, safe(unit.interventionId()) + "-" + safe(unit.segmentId()) + "-" + aspectRatio.workflowId())
                .withConditioningReferences(List.of(
                        new VisualConditioningReference(previousAssetId, "Frame anterior", previous,
                                VisualConditioningRole.PREVIOUS_FRAME, 1.0),
                        new VisualConditioningReference(nextAssetId, "Frame siguiente", next,
                                VisualConditioningRole.NEXT_FRAME, 1.0)));
        Path output;
        try (AiModelResourceGuard.Lease ignored = resourceGuard.acquire(AiModelResourceKind.IMAGE, unit.interventionId())) {
            ComfyUiWorkflowSpec workflow = workflowSpec(preset, request);
            VisualEngineResult result = visualEngineClient.generate(settings.baseUrl(), fluxTimeout(settings, preset), request, workflow,
                    attemptPolicy(), GenerationTaskKind.IMAGE_CANDIDATE, progress);
            output = result.outputPath();
        }
        var imported = services.storyboard().importImageAsset().importImage(session.project(), projectFile, output);
        session.replaceProject(imported.project(), true);
        return new TheatreGeneratedImageCandidate(unit.interventionId(), unit.sceneId(), unit.interventionId(),
                unit.segmentId(), imported.imageAsset().id(), output, false);
    }

    public TheatreGeneratedFrameCandidate generateRifeTransition(ProjectSession session,
                                                                 TheatreImageGenerationUnit current,
                                                                 TheatreImageGenerationUnit next,
                                                                 TheatrePrimaryVisualReference previousReference,
                                                                 TheatrePrimaryVisualReference nextReference,
                                                                 ComfyUiConnectionSettings settings,
                                                                 Consumer<String> progress) throws IOException {
        if (current == null || next == null || previousReference == null || nextReference == null) {
            throw new IOException("El frame RIFE requiere dos intervenciones consecutivas con imagen principal.");
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de generar frames RIFE."));
        Path projectRoot = projectFile.toAbsolutePath().normalize().getParent();
        Path outputDirectory = projectRoot.resolve("generated/teatro-ia/intermedios/rife");
        String from = current.interventionId();
        String to = next.interventionId();
        Path output;
        try (AiModelResourceGuard.Lease ignored = resourceGuard.acquire(AiModelResourceKind.IMAGE, from + "->" + to)) {
            VisualEngineResult result = visualEngineClient.interpolateMiddleFrame(
                    settings.baseUrl(),
                    settings.timeout(),
                    previousReference.absolutePath(),
                    nextReference.absolutePath(),
                    outputDirectory,
                    safe(from) + "-to-" + safe(to) + "-rife",
                    progress);
            output = result.outputPath();
        }
        var imported = services.storyboard().importImageAsset().importImage(session.project(), projectFile, output);
        session.replaceProject(imported.project(), true);
        return new TheatreGeneratedFrameCandidate(
                from + "#rife-intermediate",
                current.sceneId(),
                from,
                current.segmentId(),
                2,
                true,
                to,
                imported.imageAsset().id(),
                output,
                false);
    }

    public TheatreGeneratedImageCandidate approve(ProjectSession session, TheatreGeneratedImageCandidate candidate) {
        try {
            var script = session.narrationScript()
                    .orElseThrow(() -> new IOException("Prepara la lectura antes de asignar una imagen IA."));
            var generatedAsset = session.project().assets().byId(candidate.assetId())
                    .orElseThrow(() -> new IOException("No existe el asset generado: " + candidate.assetId()));
            var result = services.storyboard().upsertTheatreGeneratedFrameVariant().execute(
                    session.project(), session.storyboard().orElse(null), script,
                    candidate.segmentId(), generatedAsset, true);
            session.replaceProject(result.project(), true);
            session.setStoryboard(result.storyboard());
            return new TheatreGeneratedImageCandidate(candidate.unitId(), candidate.sceneId(), candidate.interventionId(),
                    candidate.segmentId(), candidate.assetId(), candidate.outputPath(), true);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private static String safe(String value) { return (value == null ? "unidad" : value.toLowerCase().replaceAll("[^a-z0-9._-]+", "-")).replaceAll("-+", "-"); }

    private GenerationAttemptPolicy attemptPolicy() throws IOException {
        return GenerationAttemptPolicy.fromSettings(services.settings().loadOperationalSettings().load());
    }

    static VisualEngineRequest visualEngineRequest(TheatreImageGenerationUnit unit,
                                                   TheatreImageGenerationPreset preset,
                                                   ImageEnhancementOutputProfile outputProfile,
                                                   TheatreImageAspectRatio aspectRatio,
                                                   Path outputDirectory,
                                                   String filenamePrefix) throws IOException {
        TheatreImageGenerationPreset selectedPreset = preset == null ? TheatreImageGenerationPreset.TEST_4GB_SD15 : preset;
        ImageEnhancementOutputProfile selectedProfile = outputProfile == null ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        TheatreImageAspectRatio selectedAspect = aspectRatio == null ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio;
        ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forPresetId(selectedPreset.name());
        if (!support.builtInWorkflowAvailable()) {
            throw new IOException(support.userMessage() + "\n" + support.diagnostic());
        }
        int targetWidth = selectedAspect.widthFor(selectedProfile);
        int targetHeight = selectedAspect.heightFor(selectedProfile);
        return new VisualEngineRequest(
                positivePrompt(unit, selectedAspect),
                "low quality, blurry, deformed hands, duplicated faces, unreadable text, watermark",
                support.checkpointName(),
                selectedPreset.steps(),
                selectedPreset.cfg(),
                selectedPreset.batchSize(),
                targetWidth,
                targetHeight,
                outputDirectory,
                filenamePrefix);
    }

    VisualEngineRequest visualEngineRequest(ProjectSession session,
                                            TheatreImageGenerationUnit unit,
                                            TheatreImageGenerationPreset preset,
                                            ImageEnhancementOutputProfile outputProfile,
                                            TheatreImageAspectRatio aspectRatio,
                                            Path outputDirectory,
                                            String filenamePrefix) throws IOException {
        VisualEngineRequest base = visualEngineRequest(unit, preset, outputProfile, aspectRatio,
                outputDirectory, filenamePrefix);
        TheatreVisualGenerationContext context = generationContext(session, unit);
        if (context.cameraGuide() != null && !context.cameraGuide().label().isBlank()) {
            base = base.withPrompt(base.prompt() + ", camera plan: " + context.cameraGuide().label());
        }
        return base.withConditioningReferences(context.conditioningReferences());
    }

    static ComfyUiWorkflowSpec workflowSpec(TheatreImageGenerationPreset preset, VisualEngineRequest request) throws IOException {
        TheatreImageGenerationPreset selected = preset == null ? TheatreImageGenerationPreset.TEST_4GB_SD15 : preset;
        if (selected == TheatreImageGenerationPreset.PRODUCTION_SDXL_REFERENCE) {
            return ComfyUiWorkflowSpec.sdxlReference(request.checkpointName());
        }
        if (!selected.fluxCompatible()) {
            return ComfyUiWorkflowSpec.sd15();
        }
        Path root = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        if (!new FluxLicenseAcceptanceStore().accepted(root)) {
            throw new IOException("Confirma la licencia FLUX.1-Kontext-dev en Configuracion antes de generar.");
        }
        FluxModelBundle bundle = selected == TheatreImageGenerationPreset.ADVANCED_FLUX_KONTEXT
                ? FluxModelBundle.inspectKontext(root)
                : FluxModelBundle.inspect(root);
        if (!bundle.ready()) {
            throw new IOException("Faltan componentes FLUX: " + String.join(", ", bundle.missingComponents()));
        }
        FluxMemoryPreflight.Report memory = new FluxMemoryPreflight().inspect(root);
        if (!memory.ready()) {
            throw new IOException(memory.userMessage() + "\n" + memory.diagnostic());
        }
        return selected == TheatreImageGenerationPreset.ADVANCED_FLUX_KONTEXT
                ? ComfyUiWorkflowSpec.fluxKontextForTarget(
                        bundle.modelName(), bundle.vaeName(), bundle.clipLName(), bundle.t5Name(),
                        request.targetWidth(), request.targetHeight())
                : ComfyUiWorkflowSpec.fluxForTarget(
                        bundle.modelName(), bundle.vaeName(), bundle.clipLName(), bundle.t5Name(),
                        request.targetWidth(), request.targetHeight());
    }

    private static java.time.Duration fluxTimeout(ComfyUiConnectionSettings settings,
                                                  TheatreImageGenerationPreset preset) {
        long configuredSeconds = settings == null || settings.timeout() == null
                ? 0
                : settings.timeout().toSeconds();
        return java.time.Duration.ofSeconds(Math.max(
                ImageGenerationSettings.DEFAULT_TIMEOUT_SECONDS,
                configuredSeconds));
    }

    private static String positivePrompt(TheatreImageGenerationUnit unit, TheatreImageAspectRatio aspectRatio) {
        TheatreImageGenerationUnit safeUnit = unit == null
                ? new TheatreImageGenerationUnit("", "", "", "", "unidad", "segmento", "", "", null)
                : unit;
        String spatialContext = safeUnit.spatialContextText() == null || safeUnit.spatialContextText().isBlank()
                ? ""
                : ", spatial blocking context: " + safeUnit.spatialContextText();
        return "theatrical aviation comedy scene, " + safeUnit.sceneName() + ", " + safeUnit.fullText()
                + spatialContext
                + ", " + aspectRatio.promptText()
                + ", consistent characters, vintage stage photography, warm cinematic light";
    }

    private static ComfyUiVisualEngineClient visualClientFrom(ApplicationServices services) {
        if (services == null || services.visual() == null) {
            return new ComfyUiVisualEngineClient();
        }
        return services.visual().comfyUiVisualEngineClient();
    }

    private static void progress(Consumer<String> progress, String message) {
        if (progress != null) {
            progress.accept(message);
        }
    }
}
