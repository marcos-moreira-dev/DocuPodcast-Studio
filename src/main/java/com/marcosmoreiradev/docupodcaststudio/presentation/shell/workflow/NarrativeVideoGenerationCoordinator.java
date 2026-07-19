package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeEnvironmentReport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ComfyUiRuntimeCapabilities;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupport;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ImageEnginePresetSupportPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeDocumentContext;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeDocumentContextCompiler;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVisualGenerationContextProvider;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVisualGenerationInput;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationTaskKind;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiSystemStats;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiVisualEngineClient;
import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiWorkflowSpec;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualClipGenerationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualClipGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualClipGenerationResult;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualClipGenerationWorkflow;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBackendResolver;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBinding;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBindingVerifier;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningReference;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualConditioningRole;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineRequest;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineRequestMapper;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualEngineResult;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGenerationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualImageWorkflowResolver;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualOutputTarget;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualReferenceBundle;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextReference;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeContextRole;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeParagraphTake;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.NarrativeVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Narrative adapter over the category-neutral local image and image-to-video infrastructure.
 */
public final class NarrativeVideoGenerationCoordinator {
    private static final String NEGATIVE_PROMPT =
            "low quality, blurry, flicker, jitter, deformed anatomy, duplicated subjects, "
                    + "inconsistent face, unreadable text, watermark, logo";

    private final ApplicationServices services;
    private final NarrativeVideoAssetWorkflow assets;
    private final NarrativeDocumentContextCompiler contextCompiler;
    private final ComfyUiVisualEngineClient client;

    public NarrativeVideoGenerationCoordinator(ApplicationServices services,
                                               NarrativeVideoAssetWorkflow assets,
                                               NarrativeDocumentContextCompiler contextCompiler) {
        this.services = Objects.requireNonNull(services, "services");
        this.assets = Objects.requireNonNull(assets, "assets");
        this.contextCompiler = Objects.requireNonNull(contextCompiler, "contextCompiler");
        this.client = services.visual().comfyUiVisualEngineClient();
    }

    public KeyframeOutcome generateKeyframe(ProjectSession session,
                                            ReadableDocument document,
                                            String blockId,
                                            Consumer<String> progress) throws IOException {
        return generateKeyframe(session, document, blockId, progress, () -> false);
    }

    public KeyframeOutcome generateKeyframe(ProjectSession session,
                                            ReadableDocument document,
                                            String blockId,
                                            Consumer<String> progress,
                                            BooleanSupplier cancellationRequested) throws IOException {
        checkCancelled(cancellationRequested);
        Path root = projectRoot(session);
        NarrativeDocumentContext context = contextCompiler.compile(document, blockId);
        NarrativeVideoConfiguration configuration = session.project().narrative().videoConfiguration();
        VisualGenerationProfile profile = VisualGenerationProfile.from(configuration.imageProfile());
        RuntimeBinding runtime = verifiedRuntime(true);
        VisualReferenceBundle references = references(session, root);
        long seed = positiveSeed(
                context.documentFingerprint(),
                blockId,
                Long.toString(System.nanoTime()));
        String prompt = keyframePrompt(context);
        String fingerprint = fingerprint(
                "keyframe", context.documentFingerprint(), blockId, configuration.toString(),
                referencesFingerprint(session), prompt, Long.toString(seed));
        VisualOutputTarget output = new VisualOutputTarget(
                root,
                Path.of("generated/narrative/keyframes/.staging"),
                "narrative-" + safe(blockId) + "-" + Long.toUnsignedString(seed, 36),
                true);
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("blockId", blockId);
        metadata.put("documentFingerprint", context.documentFingerprint());
        metadata.put("seed", Long.toString(seed));
        NarrativeVisualGenerationInput source = new NarrativeVisualGenerationInput(
                prompt,
                NEGATIVE_PROMPT,
                references,
                configuration.width(),
                configuration.height(),
                profile,
                runtime.binding(),
                output,
                Map.copyOf(metadata));
        var publicRequest = new NarrativeVisualGenerationContextProvider().build(source);
        ImageEnginePresetSupport support = ImageEnginePresetSupportPolicy.forProfile(profile);
        if (!support.builtInWorkflowAvailable()) {
            throw new IOException(support.userMessage() + "\n" + support.diagnostic());
        }
        VisualEngineRequest engineRequest = new VisualEngineRequestMapper().map(
                publicRequest,
                support.checkpointName(),
                support.defaultSteps(),
                support.defaultCfg(),
                support.defaultBatchSize(),
                output.filenamePrefix());
        ComfyUiWorkflowSpec workflow = new VisualImageWorkflowResolver().resolve(profile, engineRequest);
        progress(progress, "Generando imagen clave de " + blockId + " con " + profile.displayName() + ".");
        VisualEngineResult generated = client.generate(
                runtime.settings().imageGeneration().baseUrl(),
                runtime.timeout(),
                engineRequest,
                workflow,
                GenerationAttemptPolicy.fromSettings(runtime.settings()),
                GenerationTaskKind.IMAGE_FRAME,
                progress);
        try {
            ProjectAssetReference asset = assets.commitGeneratedKeyframe(
                    session,
                    blockId,
                    generated.outputPath(),
                    prompt,
                    NEGATIVE_PROMPT,
                    seed,
                    fingerprint);
            return new KeyframeOutcome(asset, seed, fingerprint,
                    "Imagen clave generada para " + blockId + ".");
        } finally {
            Files.deleteIfExists(generated.outputPath());
        }
    }

    public ClipOutcome generateClips(ProjectSession session,
                                     ReadableDocument document,
                                     NarrationScriptDocument script,
                                     List<AudioJobSnapshot> jobs,
                                     String blockId,
                                     Consumer<String> progress) throws IOException {
        return generateClips(session, document, script, jobs, blockId, progress, () -> false);
    }

    public ClipOutcome generateClips(ProjectSession session,
                                     ReadableDocument document,
                                     NarrationScriptDocument script,
                                     List<AudioJobSnapshot> jobs,
                                     String blockId,
                                     Consumer<String> progress,
                                     BooleanSupplier cancellationRequested) throws IOException {
        checkCancelled(cancellationRequested);
        Path root = projectRoot(session);
        NarrativeParagraphTake take = session.project().narrative().takeOrDefault(blockId);
        if (!take.enabled()) {
            throw new IOException("El parrafo " + blockId + " esta deshabilitado.");
        }
        if (!take.keyframeReady() || take.stale()) {
            throw new IOException("Genera una imagen clave vigente antes de producir clips para " + blockId + ".");
        }
        Path startFrame = resolveAsset(session, root, take.keyframeAssetId(), "imagen clave");
        double audioSeconds = audioDuration(blockId, script, jobs);
        NarrativeDocumentContext context = contextCompiler.compile(document, blockId);
        NarrativeVideoConfiguration configuration = session.project().narrative().videoConfiguration();
        VisualClipGenerationProfile profile = VisualClipGenerationProfile.from(configuration.videoProfile());
        RuntimeBinding runtime = verifiedRuntime(true);
        VisualReferenceBundle references = references(session, root);
        String prompt = clipPrompt(context);
        String sourceFingerprint = fingerprint(
                "clips", context.documentFingerprint(), blockId, configuration.toString(),
                referencesFingerprint(session), take.keyframeAssetId(),
                Double.toString(audioSeconds), prompt);
        VisualClipGenerationWorkflow workflow = new VisualClipGenerationWorkflow(client);
        ArrayList<VisualClipGenerationResult> results = new ArrayList<>();
        double remaining = audioSeconds;
        int order = 0;
        try {
            while (remaining > 0.001) {
                checkCancelled(cancellationRequested);
                double duration = Math.min(configuration.maxClipDurationSeconds(), remaining);
                long seed = positiveSeed(
                        sourceFingerprint,
                        Integer.toString(order),
                        Long.toString(System.nanoTime()));
                String prefix = "clip-" + String.format("%03d", order + 1)
                        + "-" + Long.toUnsignedString(seed, 36);
                VisualOutputTarget target = new VisualOutputTarget(
                        root,
                        Path.of("generated/narrative/clips").resolve(safe(blockId)),
                        prefix,
                        true);
                LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
                metadata.put("consumer", "narrative-video");
                metadata.put("blockId", blockId);
                metadata.put("clipOrder", Integer.toString(order));
                metadata.put("documentFingerprint", context.documentFingerprint());
                var request = new VisualClipGenerationRequest(
                        prompt,
                        NEGATIVE_PROMPT,
                        startFrame,
                        configuration.width(),
                        configuration.height(),
                        configuration.framesPerSecond(),
                        duration,
                        seed,
                        references,
                        profile,
                        null,
                        runtime.binding(),
                        target,
                        Map.copyOf(metadata));
                progress(progress, "Generando clip " + (order + 1) + " para " + blockId + ".");
                VisualClipGenerationResult result = workflow.generate(
                        runtime.settings().imageGeneration().baseUrl(),
                        runtime.timeout(),
                        request,
                        configuration.customWorkflowPath(),
                        progress);
                results.add(result);
                startFrame = result.lastFramePath();
                remaining -= duration;
                order++;
            }
        } catch (IOException ex) {
            if (!results.isEmpty() && cancellationRequested != null
                    && cancellationRequested.getAsBoolean()) {
                assets.commitGeneratedClips(session, blockId, results, sourceFingerprint);
                NarrativeParagraphTake partial = session.project().narrative()
                        .takeOrDefault(blockId)
                        .markStale();
                assets.update(session, session.project().narrative().withTake(partial));
            }
            throw ex;
        }
        assets.commitGeneratedClips(session, blockId, results, sourceFingerprint);
        return new ClipOutcome(List.copyOf(results), audioSeconds, sourceFingerprint,
                results.size() + " clip(s) generados para " + blockId + ".");
    }

    private static void checkCancelled(BooleanSupplier cancellationRequested) throws IOException {
        if (cancellationRequested != null && cancellationRequested.getAsBoolean()) {
            throw new IOException("Generacion narrativa cancelada por el usuario.");
        }
    }

    private RuntimeBinding verifiedRuntime(boolean visualWork) throws IOException {
        OperationalSettings settings = services.settings().loadOperationalSettings().load();
        ComputeEnvironmentReport environment = services.settings().inspectComputeEnvironment().inspect(settings);
        VisualComputeBinding binding;
        try {
            binding = new VisualComputeBackendResolver().resolve(
                    settings.compute(),
                    environment.devices(),
                    ComfyUiRuntimeCapabilities.unknown("Endpoint ComfyUI configurado externamente."));
        } catch (IllegalStateException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
        if (visualWork && binding.gpu() && !settings.compute().allowGpuForVideo()) {
            throw new IOException("El dispositivo seleccionado es GPU, pero su uso para video esta deshabilitado "
                    + "en Configuracion.");
        }
        Duration timeout = Duration.ofSeconds(settings.imageGeneration().timeoutSeconds());
        ComfyUiSystemStats stats = client.systemStats(settings.imageGeneration().baseUrl(), timeout);
        VisualComputeBindingVerifier.Verification verification =
                new VisualComputeBindingVerifier().verify(binding, stats);
        if (!verification.matches()) {
            throw new IOException(verification.message());
        }
        progress(null, verification.message());
        return new RuntimeBinding(settings, binding, timeout);
    }

    private VisualReferenceBundle references(ProjectSession session, Path root) throws IOException {
        ArrayList<VisualConditioningReference> resolved = new ArrayList<>();
        for (NarrativeContextReference reference : session.project().narrative().contextReferences()) {
            if (!reference.enabled()) {
                continue;
            }
            ProjectAssetReference asset = session.project().assets().byId(reference.assetId())
                    .orElseThrow(() -> new IOException(
                            "La referencia global '" + reference.displayName() + "' no tiene asset resoluble."));
            Path path = root.resolve(asset.relativePath()).normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path) || !asset.isImage()) {
                throw new IOException("La referencia global '" + reference.displayName()
                        + "' no existe dentro del proyecto.");
            }
            resolved.add(new VisualConditioningReference(
                    asset.id(),
                    reference.displayName(),
                    path,
                    role(reference.role()),
                    reference.strength()));
        }
        return new VisualReferenceBundle(List.copyOf(resolved));
    }

    private static VisualConditioningRole role(NarrativeContextRole role) {
        return switch (role) {
            case IDENTITY -> VisualConditioningRole.IDENTITY;
            case OBJECT -> VisualConditioningRole.OBJECT;
            case ENVIRONMENT -> VisualConditioningRole.ENVIRONMENT;
            case STYLE -> VisualConditioningRole.STYLE;
        };
    }

    private static Path resolveAsset(ProjectSession session,
                                     Path root,
                                     String assetId,
                                     String label) throws IOException {
        ProjectAssetReference asset = session.project().assets().byId(assetId)
                .orElseThrow(() -> new IOException("No se encontro el asset de " + label + "."));
        Path path = root.resolve(asset.relativePath()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new IOException("El asset de " + label + " no existe dentro del proyecto.");
        }
        return path;
    }

    private static double audioDuration(String blockId,
                                        NarrationScriptDocument script,
                                        List<AudioJobSnapshot> jobs) throws IOException {
        if (script == null) {
            throw new IOException("Prepara la narracion antes de generar clips.");
        }
        List<String> segmentIds = script.segments().stream()
                .filter(NarrationSegment::narratable)
                .filter(segment -> segment.sourceBlockIds().contains(blockId))
                .map(NarrationSegment::id)
                .toList();
        if (segmentIds.isEmpty()) {
            throw new IOException("El parrafo " + blockId + " no tiene una unidad de narracion.");
        }
        AudioJobSnapshot job = (jobs == null ? List.<AudioJobSnapshot>of() : jobs).stream()
                .filter(candidate -> containsCompletedAudio(candidate, segmentIds))
                .max(Comparator.comparing(AudioJobSnapshot::updatedAt))
                .orElseThrow(() -> new IOException(
                        "Renderiza primero la voz en off del parrafo " + blockId + "."));
        double duration = job.segments().stream()
                .filter(AudioSegmentSnapshot::completed)
                .filter(segment -> matchesAny(segment.segmentId(), segmentIds))
                .mapToDouble(AudioSegmentSnapshot::durationSeconds)
                .sum();
        if (duration <= 0.0) {
            throw new IOException("El audio de " + blockId + " no tiene duracion util.");
        }
        return duration;
    }

    private static boolean containsCompletedAudio(AudioJobSnapshot job, List<String> segmentIds) {
        return job != null && job.segments().stream()
                .anyMatch(segment -> segment.completed() && matchesAny(segment.segmentId(), segmentIds));
    }

    private static boolean matchesAny(String persistedId, List<String> scriptIds) {
        return scriptIds.stream().anyMatch(id -> persistedId.equals(id) || persistedId.startsWith(id + "-"));
    }

    private String referencesFingerprint(ProjectSession session) {
        StringBuilder value = new StringBuilder();
        session.project().narrative().contextReferences().stream()
                .sorted(Comparator.comparing(NarrativeContextReference::id))
                .forEach(reference -> value.append(reference.id()).append('|')
                        .append(reference.assetId()).append('|')
                        .append(reference.role()).append('|')
                        .append(reference.enabled()).append('|')
                        .append(reference.strength()).append('\n'));
        return fingerprint(value.toString());
    }

    private static String keyframePrompt(NarrativeDocumentContext context) {
        return "Vertical 9:16 narrative key frame for a short voice-over video. "
                + "Represent only information present in the following Word document context. "
                + "Preserve faces, identity, wardrobe, objects, environment and visual style from the enabled "
                + "global references. Use a coherent cinematic composition without captions or text overlays.\n\n"
                + context.promptContext();
    }

    private static String clipPrompt(NarrativeDocumentContext context) {
        return "Animate this narrative shot with subtle, coherent subject and camera motion. "
                + "Preserve faces, identity, anatomy, wardrobe, objects, background and style from the starting "
                + "frame and enabled global references. Avoid cuts, flicker and sudden scene changes. "
                + "The action must represent only this Word paragraph:\n\n" + context.paragraphText();
    }

    private static Path projectRoot(ProjectSession session) throws IOException {
        Path projectFile = Objects.requireNonNull(session, "session").projectFile()
                .orElseThrow(() -> new IOException(
                        "Guarda el proyecto antes de generar contenido narrativo."));
        return projectFile.toAbsolutePath().normalize().getParent();
    }

    private static long positiveSeed(String... values) {
        String hash = fingerprint(values);
        return Long.parseUnsignedLong(hash.substring(0, 15), 16);
    }

    private static String fingerprint(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }

    private static String safe(String value) {
        String normalized = value == null ? "" : value.strip()
                .replaceAll("[^A-Za-z0-9_-]", "-")
                .replaceAll("-{2,}", "-");
        return normalized.isBlank() ? "paragraph" : normalized;
    }

    private static void progress(Consumer<String> consumer, String message) {
        if (consumer != null && message != null && !message.isBlank()) {
            consumer.accept(message);
        }
    }

    private record RuntimeBinding(
            OperationalSettings settings,
            VisualComputeBinding binding,
            Duration timeout
    ) {
    }

    public record KeyframeOutcome(
            ProjectAssetReference asset,
            long seed,
            String sourceFingerprint,
            String message
    ) {
    }

    public record ClipOutcome(
            List<VisualClipGenerationResult> clips,
            double audioDurationSeconds,
            String sourceFingerprint,
            String message
    ) {
        public ClipOutcome {
            clips = clips == null ? List.of() : List.copyOf(clips);
        }
    }
}
