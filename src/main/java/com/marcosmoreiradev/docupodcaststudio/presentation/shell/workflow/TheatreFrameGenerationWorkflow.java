package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageAspectStrategy;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationTaskKind;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.ComfyUiConnectionSettings;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.FrameGenerationMode;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFrameGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreFrameGenerationResult;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreGeneratedFrameCandidate;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationPreset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningReference;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningRole;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineRequest;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineResult;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceRequirement;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceScheduler;
import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Generates reviewable theatrical still frames from intervention context. */
public final class TheatreFrameGenerationWorkflow {
    private final WorkspaceApplicationServices services;
    private final TheatreGenerationUnitPlanner planner = new TheatreGenerationUnitPlanner();
    private final TheatreImageGenerationWorkflow imageWorkflow;
    private final ComfyUiVisualEngineClient visualEngineClient;
    private final ResourceScheduler resourceScheduler;

    public TheatreFrameGenerationWorkflow(WorkspaceApplicationServices services, ResourceScheduler resourceScheduler) {
        this(services, visualClientFrom(services), resourceScheduler);
    }

    public TheatreFrameGenerationWorkflow(WorkspaceApplicationServices services,
                                          ComfyUiVisualEngineClient visualEngineClient,
                                          ResourceScheduler resourceScheduler) {
        this.services = Objects.requireNonNull(services, "services");
        this.visualEngineClient = Objects.requireNonNull(visualEngineClient, "visualEngineClient");
        this.resourceScheduler = Objects.requireNonNull(resourceScheduler, "resource scheduler");
        this.imageWorkflow = new TheatreImageGenerationWorkflow(services, visualEngineClient, resourceScheduler);
    }

    public Estimate estimate(ProjectSession session, NarrationScriptDocument script, TheatreFrameGenerationRequest request) {
        List<TheatreImageGenerationUnit> units = units(session, script, request);
        int requestedFrames = units.size();
        int transitionFrames = request.mode() == FrameGenerationMode.DOUBLE_STOP_MOTION ? Math.max(0, units.size() - 1) : 0;
        int pendingFrames = request.mode() == FrameGenerationMode.DOUBLE_STOP_MOTION && !supportsIntermediateGeneration(request.preset())
                ? transitionFrames
                : 0;
        return new Estimate(units.size(), requestedFrames + transitionFrames, pendingFrames);
    }

    public TheatreFrameGenerationResult generate(ProjectSession session,
                                                 NarrationScriptDocument script,
                                                 TheatreFrameGenerationRequest request,
                                                 Consumer<String> progress,
                                                 BooleanSupplier cancelled) throws IOException {
        if (session == null) {
            throw new IOException("Abre un proyecto antes de generar frames teatrales.");
        }
        Path projectFile = session.projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de generar frames teatrales."));
        Path outputRoot = request.outputDirectory();
        if (outputRoot == null || !Files.isDirectory(outputRoot)) {
            throw new IOException("Selecciona una carpeta de salida para los frames.");
        }
        List<TheatreImageGenerationUnit> units = units(session, script, request);
        Path root = outputRoot.resolve(safe(session.title()));
        if (!request.overwriteExisting()) {
            root = uniqueDirectory(root);
        }
        Files.createDirectories(root);

        ArrayList<TheatreGeneratedFrameCandidate> candidates = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        int pending = 0;
        try (ResourceLease ignored = acquireResources(
                "frames " + request.scope().kind().name().toLowerCase(Locale.ROOT))) {
            boolean needsIntermediates = request.mode() == FrameGenerationMode.DOUBLE_STOP_MOTION;
            boolean supportsIntermediates = !needsIntermediates || supportsIntermediateGeneration(request.preset());
            if (needsIntermediates && !supportsIntermediates) {
                pending += Math.max(0, units.size() - 1);
                warnings.add("Intermedios entre intervenciones no disponibles para " + request.preset().displayName()
                        + ". Usa un workflow local con image-to-image/referencias A-B desde Configuracion.");
            }
            boolean[] principalGenerated = new boolean[units.size()];
            TheatreGeneratedFrameCandidate[] principalCandidates = new TheatreGeneratedFrameCandidate[units.size()];
            boolean cancelledBeforeIntermediates = false;
            for (int i = 0; i < units.size(); i++) {
                if (isCancelled(cancelled)) {
                    warnings.add("Generacion cancelada. Los frames ya creados se conservaron.");
                    pending += units.size() - i;
                    cancelledBeforeIntermediates = true;
                    break;
                }
                TheatreImageGenerationUnit unit = units.get(i);
                if (!hasGenerationInput(session, unit)) {
                    warnings.add(unit.interventionId() + " omitida: no hay texto ni contexto visual suficiente.");
                    pending++;
                    continue;
                }
                progress(progress, "Generando frame principal de " + unit.interventionId() + ".");
                TheatreGeneratedFrameCandidate generated = generateFrame(session, projectFile, request.connectionSettings(), request.preset(),
                        request.outputProfile(), request.aspectRatio(), root, unit, 1, false, "", List.of(), progress);
                candidates.add(generated);
                principalCandidates[i] = generated;
                principalGenerated[i] = true;
            }

            if (needsIntermediates && supportsIntermediates) {
                if (cancelledBeforeIntermediates) {
                    pending += Math.max(0, units.size() - 1);
                    warnings.add("Intermedios omitidos por cancelacion.");
                } else {
                    for (int i = 0; i + 1 < units.size(); i++) {
                        if (isCancelled(cancelled)) {
                            warnings.add("Generacion cancelada. Los intermedios ya creados se conservaron.");
                            pending += (units.size() - 1) - i;
                            break;
                        }
                        TheatreImageGenerationUnit unit = units.get(i);
                        TheatreImageGenerationUnit next = units.get(i + 1);
                        if (!principalGenerated[i] || !principalGenerated[i + 1]) {
                            warnings.add("Intermedio pendiente " + unit.interventionId() + " -> " + next.interventionId()
                                    + ": faltan frames principales.");
                            pending++;
                            continue;
                        }
                        TheatreImageGenerationUnit transition = transitionUnit(unit, next);
                        progress(progress, "Generando frame intermedio " + unit.interventionId() + " -> " + next.interventionId() + ".");
                        List<VisualConditioningReference> references = transitionReferences(
                                principalCandidates[i], principalCandidates[i + 1], unit.interventionId(), next.interventionId());
                        candidates.add(generateFrame(session, projectFile, request.connectionSettings(), request.preset(),
                                request.outputProfile(), request.aspectRatio(), root, transition, 2, true, next.interventionId(),
                                references, progress));
                    }
                }
            }
        }
        Path manifest = writeManifest(root, request, candidates, warnings);
        return new TheatreFrameGenerationResult(root, units.size(), candidates.size(), pending, warnings, candidates, manifest);
    }

    private ResourceLease acquireResources(String operation) throws IOException {
        try {
            return resourceScheduler.acquire(ResourceRequirement.of(ResourceId.MODEL_MEMORY, ResourceId.GPU),
                    CancellationToken.NONE);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("La generación se interrumpió mientras esperaba recursos locales.", exception);
        }
    }

    private List<TheatreImageGenerationUnit> units(ProjectSession session, NarrationScriptDocument script, TheatreFrameGenerationRequest request) {
        if (request == null) {
            return List.of();
        }
        return planner.units(session, script, request.scope().asContextScope());
    }

    private boolean hasGenerationInput(ProjectSession session, TheatreImageGenerationUnit unit) {
        return !imageWorkflow.contextAssets(session, unit).isEmpty()
                || !normalize(unit.fullText()).isBlank()
                || !normalize(unit.spatialContextText()).isBlank();
    }

    private TheatreGeneratedFrameCandidate generateFrame(ProjectSession session,
                                                         Path projectFile,
                                                         ComfyUiConnectionSettings settings,
                                                         TheatreImageGenerationPreset preset,
                                                         ImageEnhancementOutputProfile outputProfile,
                                                         com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageAspectRatio aspectRatio,
                                                         Path root,
                                                         TheatreImageGenerationUnit unit,
                                                         int frameIndex,
                                                         boolean transition,
                                                         String nextInterventionId,
                                                         List<VisualConditioningReference> transitionReferences,
                                                         Consumer<String> progress) throws IOException {
        Path interventionDir = root.resolve(safe(unit.actName()))
                .resolve(safe(unit.sceneName()))
                .resolve(safe(transition ? baseInterventionId(unit.interventionId()) : unit.interventionId()));
        Files.createDirectories(interventionDir);
        VisualEngineRequest request = transition
                ? TheatreImageGenerationWorkflow.visualEngineRequest(unit, preset, outputProfile, aspectRatio,
                        interventionDir.resolve(".raw"),
                        safe(unit.interventionId()) + "-" + safe(unit.segmentId()) + "-" + aspectRatio.workflowId())
                        .withConditioningReferences(transitionReferences)
                : imageWorkflow.visualEngineRequest(session, unit, preset, outputProfile, aspectRatio,
                        interventionDir.resolve(".raw"),
                        safe(unit.interventionId()) + "-" + safe(unit.segmentId()) + "-" + aspectRatio.workflowId());
        var workflow = TheatreImageGenerationWorkflow.workflowSpec(preset, request);
        java.time.Duration timeout = preset != null && preset.fluxCompatible()
                ? java.time.Duration.ofMinutes(45) : settings.timeout();
        VisualEngineResult result = visualEngineClient.generate(settings.baseUrl(), timeout, request, workflow,
                attemptPolicy(), GenerationTaskKind.IMAGE_FRAME, progress);
        Path generated = result.outputPath();
        Path framePath = interventionDir.resolve("frame-" + String.format(Locale.ROOT, "%03d", frameIndex) + ".png");
        Files.move(generated, framePath, StandardCopyOption.REPLACE_EXISTING);
        var imported = services.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, framePath);
        session.replaceProject(imported.project(), true);
        return new TheatreGeneratedFrameCandidate(
                unit.interventionId() + "#frame-" + frameIndex,
                unit.sceneId(),
                transition ? baseInterventionId(unit.interventionId()) : unit.interventionId(),
                unit.segmentId(),
                frameIndex,
                transition,
                nextInterventionId == null ? "" : nextInterventionId,
                imported.imageAsset().id(),
                framePath,
                false);
    }

    private static TheatreImageGenerationUnit transitionUnit(TheatreImageGenerationUnit current, TheatreImageGenerationUnit next) {
        String id = current.interventionId() + "-to-" + next.interventionId();
        String text = "Frame intermedio de continuidad. Temporal interpolation frame between the supplied previous frame and next frame. "
                + "Preserve identity, costume, props, staging and lighting from the two image references.";
        return new TheatreImageGenerationUnit(
                current.actId(),
                current.actName(),
                current.sceneId(),
                current.sceneName(),
                id,
                current.segmentId() + "-to-" + next.segmentId(),
                current.speaker() + " / " + next.speaker(),
                text,
                "",
                null);
    }

    private static List<VisualConditioningReference> transitionReferences(TheatreGeneratedFrameCandidate previous,
                                                                          TheatreGeneratedFrameCandidate next,
                                                                          String previousInterventionId,
                                                                          String nextInterventionId) {
        if (previous == null || next == null) {
            return List.of();
        }
        return List.of(
                new VisualConditioningReference(
                        previous.assetId(),
                        "Frame anterior " + previousInterventionId,
                        previous.outputPath(),
                        VisualConditioningRole.PREVIOUS_FRAME,
                        1.0),
                new VisualConditioningReference(
                        next.assetId(),
                        "Frame siguiente " + nextInterventionId,
                        next.outputPath(),
                        VisualConditioningRole.NEXT_FRAME,
                        1.0));
    }

    private static boolean supportsIntermediateGeneration(TheatreImageGenerationPreset preset) {
        return preset != null && preset.supportsInterpolation() && preset.sd15Compatible();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static String baseInterventionId(String value) {
        String text = value == null ? "" : value;
        int marker = text.indexOf("-to-");
        return marker > 0 ? text.substring(0, marker) : text;
    }

    private static Path writeManifest(Path root,
                                      TheatreFrameGenerationRequest request,
                                      List<TheatreGeneratedFrameCandidate> candidates,
                                      List<String> warnings) throws IOException {
        Path manifest = root.resolve("frames-manifest.json");
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"mode\": \"").append(escape(request.mode().name())).append("\",\n");
        json.append("  \"scope\": \"").append(escape(request.scope().kind().name())).append("\",\n");
        json.append("  \"scopeId\": \"").append(escape(request.scope().id())).append("\",\n");
        json.append("  \"preset\": \"").append(escape(request.preset().name())).append("\",\n");
        json.append("  \"outputProfile\": \"").append(escape(request.outputProfile().name())).append("\",\n");
        json.append("  \"aspectRatio\": \"").append(escape(request.aspectRatio().label())).append("\",\n");
        json.append("  \"targetWidth\": ").append(request.aspectRatio().widthFor(request.outputProfile())).append(",\n");
        json.append("  \"targetHeight\": ").append(request.aspectRatio().heightFor(request.outputProfile())).append(",\n");
        json.append("  \"aspectStrategy\": \"").append(escape(ImageAspectStrategy.OUTPAINT_TO_TARGET.name())).append("\",\n");
        json.append("  \"intermediatePolicy\": \"").append(escape(request.mode() == FrameGenerationMode.DOUBLE_STOP_MOTION
                ? "source-target-reference"
                : "none")).append("\",\n");
        json.append("  \"frames\": [\n");
        for (int i = 0; i < candidates.size(); i++) {
            TheatreGeneratedFrameCandidate candidate = candidates.get(i);
            json.append("    {")
                    .append("\"interventionId\":\"").append(escape(candidate.interventionId())).append("\",")
                    .append("\"segmentId\":\"").append(escape(candidate.segmentId())).append("\",")
                    .append("\"frameIndex\":").append(candidate.frameIndex()).append(',')
                    .append("\"transition\":").append(candidate.transitionFrame()).append(',')
                    .append("\"nextInterventionId\":\"").append(escape(candidate.nextInterventionId())).append("\",")
                    .append("\"assetId\":\"").append(escape(candidate.assetId())).append("\",")
                    .append("\"path\":\"").append(escape(candidate.outputPath().toString().replace('\\', '/'))).append("\"")
                    .append("}");
            if (i + 1 < candidates.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ],\n");
        json.append("  \"warnings\": [");
        for (int i = 0; i < warnings.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("\"").append(escape(warnings.get(i))).append("\"");
        }
        json.append("]\n");
        json.append("}\n");
        Files.writeString(manifest, json.toString(), StandardCharsets.UTF_8);
        return manifest;
    }

    private static boolean isCancelled(BooleanSupplier cancelled) {
        return cancelled != null && cancelled.getAsBoolean();
    }

    private GenerationAttemptPolicy attemptPolicy() throws IOException {
        return GenerationAttemptPolicy.fromSettings(services.administration().settings().loadOperationalSettings().load());
    }

    private static void progress(Consumer<String> progress, String message) {
        if (progress != null) {
            progress.accept(message);
        }
    }

    private static Path uniqueDirectory(Path desired) throws IOException {
        Path candidate = desired;
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = desired.resolveSibling(desired.getFileName() + "-" + suffix);
            suffix++;
        }
        return candidate;
    }

    private static String safe(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
        return normalized.isBlank() ? "sin-nombre" : normalized;
    }

    private static String escape(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }

    public record Estimate(int interventions, int frames, int pending) {
    }

    private static ComfyUiVisualEngineClient visualClientFrom(WorkspaceApplicationServices services) {
        if (services == null || services.generation().visual() == null) {
            return new ComfyUiVisualEngineClient();
        }
        return services.generation().visual().comfyUiVisualEngineClient();
    }
}
