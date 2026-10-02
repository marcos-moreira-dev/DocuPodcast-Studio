package com.marcosmoreiradev.docupodcaststudio.presentation.narrative;

import com.marcosmoreiradev.docupodcaststudio.application.compatibility.media.LegacyEnginePresetMapper;
import com.marcosmoreiradev.docupodcaststudio.application.artifacts.ProjectArtifactStore;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageSuperResolutionCoordinator;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualResolutionProfile;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeDocumentContext;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeDocumentContextCompiler;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeGeneratedVideoArtifact;
import com.marcosmoreiradev.docupodcaststudio.application.settings.LoadOperationalSettingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.SelectedMediaEngines;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.narrative.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow.NarrativeVideoAssetWorkflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Narrative product policy over provider-neutral image and video-generation capabilities. */
public final class NarrativeExperienceController {
    private static final String NEGATIVE_PROMPT =
            "low quality, blurry, flicker, jitter, deformed anatomy, duplicated subjects, "
                    + "inconsistent face, unreadable text, watermark, logo";

    private final LoadOperationalSettingsUseCase loadSettings;
    private final MediaCapabilityService media;
    private final NarrativeVideoAssetWorkflow assets;
    private final NarrativeDocumentContextCompiler contextCompiler;
    private final ProjectArtifactStore projectArtifacts;

    public NarrativeExperienceController(LoadOperationalSettingsUseCase loadSettings,
                                               MediaCapabilityService media,
                                               NarrativeVideoAssetWorkflow assets,
                                               NarrativeDocumentContextCompiler contextCompiler,
                                               ProjectArtifactStore projectArtifacts) {
        this.loadSettings = Objects.requireNonNull(loadSettings, "load settings");
        this.media = Objects.requireNonNull(media, "media capabilities");
        this.assets = Objects.requireNonNull(assets, "narrative assets");
        this.contextCompiler = Objects.requireNonNull(contextCompiler, "context compiler");
        this.projectArtifacts = Objects.requireNonNull(projectArtifacts, "project artifacts");
    }

    public KeyframeOutcome generateKeyframe(ProjectSession session, ReadableDocument document, String blockId,
                                            Consumer<String> progress) throws IOException {
        return generateKeyframe(session, document, blockId, progress, () -> false);
    }

    public KeyframeOutcome generateKeyframe(ProjectSession session, ReadableDocument document, String blockId,
                                            Consumer<String> progress, BooleanSupplier cancellation) throws IOException {
        checkCancelled(cancellation);
        Path projectFile = projectFile(session);
        Path root = projectArtifacts.projectRoot(projectFile);
        NarrativeDocumentContext context = contextCompiler.compile(document, blockId);
        NarrativeVideoConfiguration configuration = session.project().narrative().videoConfiguration();
        OperationalSettings settings = loadSettings.load();
        long seed = positiveSeed(context.documentFingerprint(), blockId, Long.toString(System.nanoTime()));
        String prompt = keyframePrompt(context);
        String sourceFingerprint = fingerprint("keyframe", context.documentFingerprint(), blockId,
                configuration.toString(), referencesFingerprint(session), prompt, Long.toString(seed));
        Path output = root.resolve("generated/narrative/keyframes/.engine-output");
        var working = VisualResolutionProfile.workingDimensionsForExact(
                configuration.width(), configuration.height());
        List<MediaReference> mediaReferences = references(session, root);
        ImageGenerationRequest request = new ImageGenerationRequest(prompt, NEGATIVE_PROMPT,
                working.width(), working.height(),
                mediaReferences.stream().map(MediaReference::file).toList(), output,
                "narrative-" + safe(blockId) + "-" + Long.toUnsignedString(seed, 36),
                Map.of("deliveryWidth", Integer.toString(configuration.width()),
                        "deliveryHeight", Integer.toString(configuration.height())),
                LegacyEnginePresetMapper.image(configuration.imageProfile()), mediaReferences, seed, 1);
        ExecutionContext execution = context("narrative-keyframe-" + blockId, settings, cancellation, progress);
        progress(progress, "Generando imagen clave neutral para " + blockId + ".");
        ImageGenerationResult result;
        try {
            result = media.generateImage(null, request, execution);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Generación narrativa cancelada.", ex);
        }
        Path rawGenerated = result.images().stream().findFirst()
                .orElseThrow(() -> new IOException("El motor no produjo una imagen clave."));
        int widthRatio = configuration.height() > configuration.width() ? 9 : 16;
        int heightRatio = configuration.height() > configuration.width() ? 16 : 9;
        ImageSuperResolutionCoordinator.Result postprocessed =
                new ImageSuperResolutionCoordinator(media).processGenerated(
                        session.project(), settings.imageSuperResolution(), rawGenerated, output,
                        "narrative-" + safe(blockId) + "-upscaled",
                        widthRatio, heightRatio, prompt, execution);
        if (!postprocessed.warning().isBlank()) progress(progress, postprocessed.warning());
        Path generated = postprocessed.output();
        try {
            ProjectAssetReference asset = assets.commitGeneratedKeyframe(session, blockId, generated, prompt,
                    NEGATIVE_PROMPT, seed, sourceFingerprint);
            return new KeyframeOutcome(asset, seed, sourceFingerprint, "Imagen clave generada para " + blockId + ".");
        } finally {
            projectArtifacts.discardProjectOwned(projectFile, generated);
        }
    }

    public ClipOutcome generateClips(ProjectSession session, ReadableDocument document, NarrationScriptDocument script,
                                     List<AudioJobSnapshot> jobs, String blockId, Consumer<String> progress) throws IOException {
        return generateClips(session, document, script, jobs, blockId, progress, () -> false);
    }

    public ClipOutcome generateClips(ProjectSession session, ReadableDocument document, NarrationScriptDocument script,
                                     List<AudioJobSnapshot> jobs, String blockId, Consumer<String> progress,
                                     BooleanSupplier cancellation) throws IOException {
        checkCancelled(cancellation);
        Path projectFile = projectFile(session);
        Path root = projectArtifacts.projectRoot(projectFile);
        NarrativeParagraphTake take = session.project().narrative().takeOrDefault(blockId);
        if (!take.enabled()) throw new IOException("El párrafo " + blockId + " está deshabilitado.");
        if (!take.keyframeReady() || take.stale()) {
            throw new IOException("Genera una imagen clave vigente antes de producir clips para " + blockId + ".");
        }
        Path startFrame = resolveAsset(session, root, take.keyframeAssetId(), "imagen clave");
        double audioSeconds = audioDuration(blockId, script, jobs);
        NarrativeDocumentContext documentContext = contextCompiler.compile(document, blockId);
        NarrativeVideoConfiguration configuration = session.project().narrative().videoConfiguration();
        OperationalSettings settings = loadSettings.load();
        EnginePresetId preset = LegacyEnginePresetMapper.video(configuration.videoProfile());
        String prompt = clipPrompt(documentContext);
        String sourceFingerprint = fingerprint("clips", documentContext.documentFingerprint(), blockId,
                configuration.toString(), referencesFingerprint(session), take.keyframeAssetId(),
                Double.toString(audioSeconds), prompt);
        ArrayList<NarrativeGeneratedVideoArtifact> results = new ArrayList<>();
        double remaining = audioSeconds;
        int order = 0;
        try {
            while (remaining > 0.001) {
                checkCancelled(cancellation);
                double duration = Math.min(configuration.maxClipDurationSeconds(), remaining);
                long seed = positiveSeed(sourceFingerprint, Integer.toString(order), Long.toString(System.nanoTime()));
                String prefix = "clip-" + String.format("%03d", order + 1) + "-" + Long.toUnsignedString(seed, 36);
                Path output = root.resolve("generated/narrative/clips").resolve(safe(blockId));
                VideoGenerationRequest request = new VideoGenerationRequest(prompt, NEGATIVE_PROMPT, startFrame,
                        references(session, root), configuration.width(), configuration.height(),
                        configuration.framesPerSecond(), duration, seed, preset, output, prefix,
                        Map.of("consumer", "narrative-video", "blockId", blockId,
                                "clipOrder", Integer.toString(order)));
                progress(progress, "Generando clip " + (order + 1) + " para " + blockId + ".");
                VideoGenerationResult generated;
                try {
                    generated = media.generateVideo(SelectedMediaEngines.from(settings).videoGeneration(), request,
                            context("narrative-video-" + blockId + "-" + order, settings, cancellation, progress));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Generación narrativa cancelada.", ex);
                }
                if (!projectArtifacts.isProjectOwnedRegularFile(projectFile, generated.continuationFrame())) {
                    throw new IOException("El motor produjo un clip pero no el frame de continuidad requerido.");
                }
                EngineId engineId = new EngineId(generated.diagnostics().getOrDefault("engineId", "unknown"));
                EnginePresetId actualPreset = new EnginePresetId(generated.diagnostics().getOrDefault("presetId", preset.value()));
                results.add(new NarrativeGeneratedVideoArtifact(generated.videoFile(), generated.continuationFrame(),
                        configuration.width(), configuration.height(), configuration.framesPerSecond(), duration,
                        engineId, actualPreset, seed, generated.diagnostics()));
                startFrame = generated.continuationFrame();
                remaining -= duration;
                order++;
            }
        } catch (IOException failure) {
            if (!results.isEmpty() && cancellation != null && cancellation.getAsBoolean()) {
                assets.commitGeneratedClips(session, blockId, results, sourceFingerprint);
                assets.update(session, session.project().narrative().withTake(
                        session.project().narrative().takeOrDefault(blockId).markStale()));
            }
            throw failure;
        }
        assets.commitGeneratedClips(session, blockId, results, sourceFingerprint);
        return new ClipOutcome(results, audioSeconds, sourceFingerprint,
                results.size() + " clip(s) generados para " + blockId + ".");
    }

    private ExecutionContext context(String id, OperationalSettings settings, BooleanSupplier cancellation,
                                     Consumer<String> progress) {
        Duration timeout = Duration.ofSeconds(settings.imageGeneration().timeoutSeconds());
        ExecutionPolicy policy = new ExecutionPolicy(timeout, settings.imageGeneration().maxAttempts());
        return new ExecutionContext(id, cancellation == null ? CancellationToken.NONE : cancellation::getAsBoolean,
                (stage, amount, message) -> progress(progress, message), policy, ResourceLease.NONE);
    }

    private List<MediaReference> references(ProjectSession session, Path root) throws IOException {
        ArrayList<MediaReference> result = new ArrayList<>();
        for (NarrativeContextReference reference : session.project().narrative().contextReferences()) {
            if (!reference.enabled()) continue;
            Path file = resolveAsset(session, root, reference.assetId(), reference.displayName());
            result.add(new MediaReference(reference.id(), file, role(reference.role()), reference.strength(),
                    Map.of("assetId", reference.assetId())));
        }
        return List.copyOf(result);
    }

    private static MediaReferenceRole role(NarrativeContextRole role) {
        return switch (role) {
            case IDENTITY -> MediaReferenceRole.IDENTITY;
            case OBJECT -> MediaReferenceRole.OBJECT;
            case ENVIRONMENT -> MediaReferenceRole.ENVIRONMENT;
            case STYLE -> MediaReferenceRole.STYLE;
        };
    }

    private Path resolveAsset(ProjectSession session, Path root, String assetId, String label) throws IOException {
        ProjectAssetReference asset = session.project().assets().byId(assetId)
                .orElseThrow(() -> new IOException("No se encontró el recurso de " + label + "."));
        try {
            return projectArtifacts.resolveExisting(projectFile(session), asset.relativePath());
        } catch (IOException exception) {
            throw new IOException("El recurso de " + label + " no existe dentro del proyecto.", exception);
        }
    }

    private static double audioDuration(String blockId, NarrationScriptDocument script,
                                        List<AudioJobSnapshot> jobs) throws IOException {
        if (script == null) throw new IOException("Prepara la narración antes de generar clips.");
        List<String> segmentIds = script.segments().stream().filter(NarrationSegment::narratable)
                .filter(segment -> segment.sourceBlockIds().contains(blockId)).map(NarrationSegment::id).toList();
        if (segmentIds.isEmpty()) throw new IOException("El párrafo " + blockId + " no tiene narración.");
        AudioJobSnapshot job = (jobs == null ? List.<AudioJobSnapshot>of() : jobs).stream()
                .filter(candidate -> containsCompletedAudio(candidate, segmentIds))
                .max(Comparator.comparing(AudioJobSnapshot::updatedAt))
                .orElseThrow(() -> new IOException("Renderiza primero la voz en off del párrafo " + blockId + "."));
        double duration = job.segments().stream().filter(AudioSegmentSnapshot::completed)
                .filter(segment -> matchesAny(segment.segmentId(), segmentIds))
                .mapToDouble(AudioSegmentSnapshot::durationSeconds).sum();
        if (duration <= 0.0) throw new IOException("El audio de " + blockId + " no tiene duración útil.");
        return duration;
    }

    private static boolean containsCompletedAudio(AudioJobSnapshot job, List<String> ids) {
        return job != null && job.segments().stream()
                .anyMatch(segment -> segment.completed() && matchesAny(segment.segmentId(), ids));
    }

    private static boolean matchesAny(String persistedId, List<String> ids) {
        return ids.stream().anyMatch(id -> persistedId.equals(id) || persistedId.startsWith(id + "-"));
    }

    private String referencesFingerprint(ProjectSession session) {
        StringBuilder value = new StringBuilder();
        session.project().narrative().contextReferences().stream()
                .sorted(Comparator.comparing(NarrativeContextReference::id))
                .forEach(reference -> value.append(reference.id()).append('|').append(reference.assetId())
                        .append('|').append(reference.role()).append('|').append(reference.enabled())
                        .append('|').append(reference.strength()).append('\n'));
        return fingerprint(value.toString());
    }

    private static String keyframePrompt(NarrativeDocumentContext context) {
        return "Vertical 9:16 narrative key frame for a short voice-over video. Represent only information "
                + "present in the document context. Preserve identity, objects, environment and style from enabled "
                + "references. Use a coherent cinematic composition without captions.\n\n" + context.promptContext();
    }

    private static String clipPrompt(NarrativeDocumentContext context) {
        return "Animate this narrative shot with subtle, coherent subject and camera motion. Preserve identity, "
                + "anatomy, objects, background and style from the starting frame and references. Avoid cuts, "
                + "flicker and sudden scene changes.\n\n" + context.paragraphText();
    }

    private static Path projectFile(ProjectSession session) throws IOException {
        return Objects.requireNonNull(session, "session").projectFile()
                .orElseThrow(() -> new IOException("Guarda el proyecto antes de generar contenido narrativo."));
    }

    private static void checkCancelled(BooleanSupplier cancellation) throws IOException {
        if (cancellation != null && cancellation.getAsBoolean()) throw new IOException("Generación narrativa cancelada.");
    }

    private static long positiveSeed(String... values) {
        return Long.parseUnsignedLong(fingerprint(values).substring(0, 15), 16);
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
            throw new IllegalStateException("SHA-256 no está disponible.", ex);
        }
    }

    private static String safe(String value) {
        String normalized = value == null ? "" : value.strip().replaceAll("[^A-Za-z0-9_-]", "-")
                .replaceAll("-{2,}", "-");
        return normalized.isBlank() ? "paragraph" : normalized;
    }

    private static void progress(Consumer<String> consumer, String message) {
        if (consumer != null && message != null && !message.isBlank()) consumer.accept(message);
    }

    public record KeyframeOutcome(ProjectAssetReference asset, long seed, String sourceFingerprint, String message) { }
    public record ClipOutcome(List<NarrativeGeneratedVideoArtifact> clips, double audioDurationSeconds,
                              String sourceFingerprint, String message) {
        public ClipOutcome { clips = clips == null ? List.of() : List.copyOf(clips); }
    }
}
