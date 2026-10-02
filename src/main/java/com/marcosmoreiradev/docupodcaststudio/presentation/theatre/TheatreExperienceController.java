package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.compatibility.media.LegacyEnginePresetMapper;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageSuperResolutionCoordinator;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ImageGenerationWorkspaceSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreVisualGenerationContextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.BuildTheatreConditionedGenerationPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreConditionedGenerationPlan;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedFrameCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedImageCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageContextAsset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatrePrimaryVisualReference;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreVisualGenerationContext;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningReference;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.TheatreGenerationUnitPlanner;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

/** Coordinates provider-neutral image generation and reviewable theatre candidates. */
public final class TheatreExperienceController {
    private final WorkspaceApplicationServices services;
    private final TheatreGenerationUnitPlanner planner = new TheatreGenerationUnitPlanner();
    private final BuildTheatreVisualGenerationContextUseCase contextBuilder = new BuildTheatreVisualGenerationContextUseCase();
    private final BuildTheatreConditionedGenerationPlanUseCase conditionedPlanBuilder =
            new BuildTheatreConditionedGenerationPlanUseCase();
    private final MediaCapabilityService mediaCapabilities;

    public TheatreExperienceController(WorkspaceApplicationServices services,
                                       MediaCapabilityService mediaCapabilities) {
        this.services = Objects.requireNonNull(services, "services");
        this.mediaCapabilities = Objects.requireNonNull(mediaCapabilities, "media capabilities");
    }

    public List<TheatreImageGenerationUnit> queue(ProjectSession session, com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument script, TheatreContextExportScope scope) {
        return planner.units(session, script, scope);
    }

    public EngineReadiness readiness() {
        EngineRegistry<ImageGenerationEngine> registry = mediaCapabilities.platform().imageEngines();
        EngineId selected = selectedEngineId();
        ImageGenerationEngine engine = selected == null
                ? registry.engines().stream().findFirst().orElse(null)
                : registry.find(selected).orElse(null);
        if (engine == null) {
            EngineId missing = selected == null ? new EngineId("image-unavailable") : selected;
            return EngineReadiness.unavailable(missing, "No hay un motor de imagen registrado.",
                    "Revisa la composición del launcher.");
        }
        return engine.inspectReadiness(new EngineConfiguration(engine.descriptor().id(), java.util.Map.of()));
    }

    public List<TheatreImageContextAsset> contextAssets(ProjectSession session, TheatreImageGenerationUnit unit) {
        return generationContext(session, unit).allAssets();
    }

    public TheatreVisualGenerationContext generationContext(ProjectSession session, TheatreImageGenerationUnit unit) {
        if (session == null || unit == null) {
            return new TheatreVisualGenerationContext(unit, List.of(), List.of(), List.of(), null, null, null);
        }
        Path root = session.projectFile().map(path -> path.toAbsolutePath().normalize().getParent()).orElse(null);
        return contextBuilder.execute(session.project(), session.storyboard().orElse(null),
                session.narrationScript().orElse(null), root, unit);
    }

    public TheatreGeneratedImageCandidate generate(ProjectSession session, TheatreImageGenerationUnit unit,
                                                   ImageGenerationWorkspaceSettings settings, TheatreImageGenerationPreset preset,
                                                   boolean assignToIntervention) throws IOException {
        return generate(session, unit, settings, preset, ImageEnhancementOutputProfile.FHD_1080,
                TheatreImageAspectRatio.WIDE_16_9, assignToIntervention, null);
    }

    public TheatreGeneratedImageCandidate generate(ProjectSession session, TheatreImageGenerationUnit unit,
                                                   ImageGenerationWorkspaceSettings settings,
                                                   TheatreImageGenerationPreset preset,
                                                   ImageEnhancementOutputProfile outputProfile,
                                                   boolean assignToIntervention) throws IOException {
        return generate(session, unit, settings, preset, outputProfile, TheatreImageAspectRatio.WIDE_16_9,
                assignToIntervention, null);
    }

    public TheatreGeneratedImageCandidate generate(ProjectSession session, TheatreImageGenerationUnit unit,
                                                   ImageGenerationWorkspaceSettings settings,
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
        Path outputDirectory = outputRoot.resolve(safe(unit.sceneId()))
                .resolve(safe(unit.interventionId()))
                .resolve(safe(unit.segmentId()));
        Path output = generateRaw(session, unit, settings, preset, outputProfile, aspectRatio, outputDirectory,
                safe(unit.interventionId()) + "-" + safe(unit.segmentId()) + "-" + aspectRatio.workflowId(),
                List.of(), progress);
        progress(progress, "Importando el candidato sin reemplazar el frame aprobado.");
        var imported = services.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, output);
        session.replaceProject(imported.project(), true);
        TheatreGeneratedImageCandidate candidate = new TheatreGeneratedImageCandidate(unit.interventionId(), unit.sceneId(), unit.interventionId(),
                unit.segmentId(), imported.imageAsset().id(), output, false);
        progress(progress, "Candidato importado y listo para revisión.");
        return assignToIntervention ? approve(session, candidate) : candidate;
    }

    public TheatreGeneratedImageCandidate generateTransition(ProjectSession session,
                                                             TheatreImageGenerationUnit unit,
                                                             String previousAssetId,
                                                             Path previousFrame,
                                                             String nextAssetId,
                                                             Path nextFrame,
                                                             ImageGenerationWorkspaceSettings settings,
                                                             TheatreImageGenerationPreset preset,
                                                             ImageEnhancementOutputProfile outputProfile,
                                                             TheatreImageAspectRatio aspectRatio,
                                                             Consumer<String> progress) throws IOException {
        outputProfile = outputProfile == null ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        aspectRatio = aspectRatio == null ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio;
        Path previous = previousFrame == null ? null : previousFrame.toAbsolutePath().normalize();
        Path next = nextFrame == null ? null : nextFrame.toAbsolutePath().normalize();
        if (previous == null || next == null) {
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
        List<MediaReference> references = List.of(
                new MediaReference(previousAssetId, previous, MediaReferenceRole.START_FRAME, 1.0,
                        java.util.Map.of("label", "Frame anterior")),
                new MediaReference(nextAssetId, next, new MediaReferenceRole("end-frame"), 1.0,
                        java.util.Map.of("label", "Frame siguiente")));
        Path output = generateRaw(session, unit, settings, preset, outputProfile, aspectRatio, outputDirectory,
                safe(unit.interventionId()) + "-" + safe(unit.segmentId()) + "-" + aspectRatio.workflowId(),
                references, progress);
        var imported = services.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, output);
        session.replaceProject(imported.project(), true);
        return new TheatreGeneratedImageCandidate(unit.interventionId(), unit.sceneId(), unit.interventionId(),
                unit.segmentId(), imported.imageAsset().id(), output, false);
    }

    public TheatreGeneratedFrameCandidate generateIntermediateTransition(ProjectSession session,
                                                                         TheatreImageGenerationUnit current,
                                                                         TheatreImageGenerationUnit next,
                                                                         TheatrePrimaryVisualReference previousReference,
                                                                         TheatrePrimaryVisualReference nextReference,
                                                                         ImageGenerationWorkspaceSettings settings,
                                                                         Consumer<String> progress) throws IOException {
        if (current == null || next == null || previousReference == null || nextReference == null) {
            throw new IOException("El frame intermedio requiere dos intervenciones consecutivas con imagen principal.");
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de generar frames intermedios."));
        Path projectRoot = projectFile.toAbsolutePath().normalize().getParent();
        Path outputDirectory = projectRoot.resolve("generated/teatro-ia/intermedios");
        String from = current.interventionId();
        String to = next.interventionId();
        TheatreImageGenerationUnit transition = new TheatreImageGenerationUnit(
                current.actId(), current.actName(), current.sceneId(), current.sceneName(),
                from + "-to-" + to, current.segmentId(), current.speaker(),
                "Transición visual coherente entre dos frames consecutivos.", current.spatialContextText(), null);
        List<MediaReference> references = List.of(
                new MediaReference(previousReference.assetId(), previousReference.absolutePath(),
                        MediaReferenceRole.START_FRAME, 1.0, java.util.Map.of()),
                new MediaReference(nextReference.assetId(), nextReference.absolutePath(),
                        new MediaReferenceRole("end-frame"), 1.0, java.util.Map.of()));
        Path output = generateRaw(session, transition, settings, TheatreImageGenerationPreset.TEST_4GB_SD15,
                ImageEnhancementOutputProfile.FHD_1080, TheatreImageAspectRatio.WIDE_16_9,
                outputDirectory, safe(from) + "-to-" + safe(to) + "-middle", references, progress,
                new EnginePresetId("middle-frame-interpolation"));
        var imported = services.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, output);
        session.replaceProject(imported.project(), true);
        return new TheatreGeneratedFrameCandidate(
                from + "#generated-intermediate",
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
            var result = services.generation().storyboard().upsertTheatreGeneratedFrameVariant().execute(
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

    public Path generateRaw(ProjectSession session,
                            TheatreImageGenerationUnit unit,
                            ImageGenerationWorkspaceSettings settings,
                            TheatreImageGenerationPreset preset,
                            ImageEnhancementOutputProfile outputProfile,
                            TheatreImageAspectRatio aspectRatio,
                            Path outputDirectory,
                            String filenamePrefix,
                            List<MediaReference> extraReferences,
                            Consumer<String> progress) throws IOException {
        return generateRaw(session, unit, settings, preset, outputProfile, aspectRatio, outputDirectory,
                filenamePrefix, extraReferences, progress, null);
    }

    private Path generateRaw(ProjectSession session,
                            TheatreImageGenerationUnit unit,
                            ImageGenerationWorkspaceSettings settings,
                            TheatreImageGenerationPreset preset,
                            ImageEnhancementOutputProfile outputProfile,
                            TheatreImageAspectRatio aspectRatio,
                            Path outputDirectory,
                            String filenamePrefix,
                            List<MediaReference> extraReferences,
                            Consumer<String> progress,
                            EnginePresetId presetOverride) throws IOException {
        TheatreVisualGenerationContext context = generationContext(session, unit);
        long seed = Math.max(0L, System.nanoTime());
        TheatreConditionedGenerationPlan conditionedPlan = presetOverride == null
                ? conditionedPlanBuilder.execute(context, aspectRatio, seed)
                : new TheatreConditionedGenerationPlan(
                positivePrompt(unit, aspectRatio),
                "low quality, blurry, deformed hands, duplicated faces, unreadable text, watermark",
                seed, List.of(), List.of(), java.util.Map.of());
        String prompt = conditionedPlan.prompt();
        ArrayList<MediaReference> references = new ArrayList<>(conditionedPlan.references());
        if (extraReferences != null) references.addAll(extraReferences);
        TheatreImageGenerationPreset effectivePreset = conditionedPlan.references().stream()
                .anyMatch(reference -> MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role()))
                ? TheatreImageGenerationPreset.CONTEXTUAL_4GB_SD15 : preset;
        ImageGenerationRequest request = imageRequest(unit, effectivePreset, outputProfile, aspectRatio,
                outputDirectory, filenamePrefix, prompt, references);
        request = new ImageGenerationRequest(
                request.prompt(), conditionedPlan.negativePrompt(), request.width(), request.height(),
                request.references(), request.outputDirectory(), request.filenamePrefix(), request.options(),
                request.presetId(), request.mediaReferences(), conditionedPlan.seed(), request.batchSize());
        if (presetOverride != null) request = withPreset(request, presetOverride);
        OperationalSettings operational = services.administration().settings().loadOperationalSettings().load();
        Duration timeout = settings == null || settings.timeout() == null
                ? Duration.ofSeconds(operational.imageGeneration().timeoutSeconds()) : settings.timeout();
        ExecutionPolicy policy = new ExecutionPolicy(timeout, operational.imageGeneration().maxAttempts());
        ExecutionContext execution = new ExecutionContext("theatre-image-" + safe(unit.interventionId()),
                CancellationToken.NONE,
                (stage, amount, message) -> progress(progress, message), policy, ResourceLease.NONE);
        progress(progress, "En cola.");
        try {
            progress(progress, "Validando contexto, personajes y variante activa del storyboard.");
            ImageGenerationResult result = mediaCapabilities.generateImage(
                    SelectedMediaEngines.from(operational).image(), request, execution);
            Path generated = result.images().stream().findFirst()
                    .orElseThrow(() -> new IOException("El motor no produjo una imagen teatral."));
            progress(progress, "Validando salida y registrando referencias, semilla y dispositivo.");
            persistProvenance(generated, conditionedPlan, result.diagnostics(), operational);
            progress(progress, "Aplicando el postproceso visual configurado para el proyecto.");
            ImageSuperResolutionCoordinator.Result postprocessed =
                    new ImageSuperResolutionCoordinator(mediaCapabilities).processGenerated(
                            session.project(), operational.imageSuperResolution(), generated,
                            outputDirectory, filenamePrefix + "-upscaled",
                            aspectRatio.widthRatio(), aspectRatio.heightRatio(),
                            request.prompt(),
                            execution);
            if (!postprocessed.warning().isBlank()) progress(progress, postprocessed.warning());
            return postprocessed.output();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Generación de imagen cancelada.", ex);
        }
    }

    private static void persistProvenance(Path image,
                                          TheatreConditionedGenerationPlan plan,
                                          java.util.Map<String, String> diagnostics,
                                          OperationalSettings settings) throws IOException {
        if (image == null || plan == null) return;
        Properties properties = new Properties();
        plan.provenance().forEach(properties::setProperty);
        if (diagnostics != null) diagnostics.forEach(
                (key, value) -> properties.setProperty("engine." + key, value == null ? "" : value));
        properties.setProperty("references", String.join("\n", plan.references().stream()
                .map(reference -> reference.role().value() + "|" + reference.id() + "|"
                        + reference.file() + "|" + reference.metadata())
                .toList()));
        properties.setProperty("computePolicy",
                settings == null ? "" : settings.compute().policy().name());
        Path sidecar = image.resolveSibling(image.getFileName() + ".provenance.properties");
        Files.createDirectories(sidecar.getParent());
        try (var writer = Files.newBufferedWriter(sidecar, StandardCharsets.UTF_8)) {
            properties.store(writer, "DocuPodcast Studio conditioned theatre generation");
        }
    }

    private static ImageGenerationRequest withPreset(ImageGenerationRequest request, EnginePresetId preset) {
        return new ImageGenerationRequest(request.prompt(), request.negativePrompt(), request.width(), request.height(),
                request.references(), request.outputDirectory(), request.filenamePrefix(), request.options(), preset,
                request.mediaReferences(), request.seed(), request.batchSize());
    }

    public static ImageGenerationRequest imageRequest(TheatreImageGenerationUnit unit,
                                                       TheatreImageGenerationPreset preset,
                                                       ImageEnhancementOutputProfile outputProfile,
                                                       TheatreImageAspectRatio aspectRatio,
                                                       Path outputDirectory,
                                                       String filenamePrefix,
                                                       String prompt,
                                                       List<MediaReference> references) {
        ImageEnhancementOutputProfile profile = outputProfile == null
                ? ImageEnhancementOutputProfile.FHD_1080 : outputProfile;
        TheatreImageAspectRatio ratio = aspectRatio == null ? TheatreImageAspectRatio.WIDE_16_9 : aspectRatio;
        TheatreImageGenerationPreset selected = preset == null
                ? TheatreImageGenerationPreset.TEST_4GB_SD15 : preset;
        int width = ratio.widthFor(profile);
        int height = ratio.heightFor(profile);
        var delivery = ratio.deliveryDimensions(profile);
        List<MediaReference> safeReferences = references == null ? List.of() : List.copyOf(references);
        return new ImageGenerationRequest(prompt,
                "low quality, blurry, deformed hands, duplicated faces, unreadable text, watermark",
                width, height, safeReferences.stream().map(MediaReference::file).toList(),
                outputDirectory, filenamePrefix,
                java.util.Map.of("deliveryWidth", Integer.toString(delivery.width()),
                        "deliveryHeight", Integer.toString(delivery.height())),
                LegacyEnginePresetMapper.image(selected.name()), safeReferences,
                Math.max(0, System.nanoTime()), selected.batchSize());
    }

    private static List<MediaReference> mediaReferences(List<VisualConditioningReference> references) {
        if (references == null) return List.of();
        return references.stream().filter(reference -> reference != null && reference.imagePath() != null)
                .map(reference -> new MediaReference(reference.assetId(), reference.imagePath(),
                        new MediaReferenceRole(reference.role().name().toLowerCase(java.util.Locale.ROOT).replace('_', '-')),
                        reference.strength(), java.util.Map.of("label", reference.label())))
                .toList();
    }

    private EngineId selectedEngineId() {
        try {
            return SelectedMediaEngines.from(services.administration().settings().loadOperationalSettings().load()).image();
        } catch (IOException | RuntimeException ex) {
            return null;
        }
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

    private static void progress(Consumer<String> progress, String message) {
        if (progress != null) {
            progress.accept(message);
        }
    }
}
