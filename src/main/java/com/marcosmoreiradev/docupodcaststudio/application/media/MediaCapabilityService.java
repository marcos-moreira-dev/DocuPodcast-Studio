package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Narrow synchronous application facade over neutral engines and the shared resource scheduler. */
public final class MediaCapabilityService {
    private final MediaEnginePlatform platform;
    private final ResourceScheduler scheduler;
    private final Supplier<ComputePreference> contentAnalysisComputePreference;
    private final Supplier<ComputePreference> voiceComputePreference;
    private final ThreadLocal<ContentBatchState> contentBatch = new ThreadLocal<>();

    public MediaCapabilityService(MediaEnginePlatform platform, ResourceScheduler scheduler) {
        this(platform, scheduler, ComputePreference::automatic,
                ComputePreference::automatic);
    }

    public MediaCapabilityService(MediaEnginePlatform platform, ResourceScheduler scheduler,
                                  Supplier<ComputePreference> contentAnalysisComputePreference) {
        this(platform, scheduler, contentAnalysisComputePreference,
                ComputePreference::automatic);
    }

    public MediaCapabilityService(MediaEnginePlatform platform, ResourceScheduler scheduler,
                                  Supplier<ComputePreference> contentAnalysisComputePreference,
                                  Supplier<ComputePreference> voiceComputePreference) {
        this.platform = Objects.requireNonNull(platform, "media engine platform");
        this.scheduler = Objects.requireNonNull(scheduler, "resource scheduler");
        this.contentAnalysisComputePreference = Objects.requireNonNullElse(
                contentAnalysisComputePreference, ComputePreference::automatic);
        this.voiceComputePreference = Objects.requireNonNullElse(
                voiceComputePreference, ComputePreference::automatic);
    }

    public VoiceSynthesisBatchResult synthesize(EngineId selected, VoiceSynthesisBatchRequest request,
                                                ExecutionContext context) throws IOException, InterruptedException {
        VoiceSynthesisEngine engine = select(platform.voiceEngines(), selected);
        ComputePreference preference = safePreference(
                voiceComputePreference, ComputePreference.automatic());
        ComputeResourceDemand demand = engine.resourceDemand(preference);
        ComputeJobPriority priority = priority(
                request.options().get("computePriority"),
                ComputeJobPriority.USER_AUDIO);
        ExecutionContext configured = withPreference(context, preference,
                "voice-synthesis");
        return withResources(admission(configured, priority,
                        ComputeWorkloadKind.VOICE_SYNTHESIS, demand), configured,
                current -> engine.synthesizeBatch(request, current));
    }

    public ImageGenerationResult generateImage(EngineId selected, ImageGenerationRequest request,
                                               ExecutionContext context) throws IOException, InterruptedException {
        ImageGenerationEngine engine = select(platform.imageEngines(), selected);
        ComputePreference preference = context.computePreference().mode() == ComputePreference.Mode.AUTO
                ? ComputePreference.preferGpu(context.computePreference().allowHostMemory()) : context.computePreference();
        ExecutionContext configured = withPreference(context, preference, "image-generation");
        return withResources(admission(configured, ComputeJobPriority.BACKGROUND,
                        ComputeWorkloadKind.IMAGE_PROCESSING,
                        engine.resourceDemand(request, configured.computePreference())), configured,
                current -> {
                    Throwable generationFailure = null;
                    try { return engine.generate(request, current); }
                    catch (IOException | InterruptedException | RuntimeException failure) {
                        generationFailure = failure;
                        throw failure;
                    } finally {
                        if (Boolean.parseBoolean(request.options().getOrDefault("releaseModelAfterRequest", "false")))
                            try { engine.releaseIdleResources(current); }
                            catch (IOException | InterruptedException cleanupFailure) {
                                if (cleanupFailure instanceof InterruptedException) Thread.currentThread().interrupt();
                                if (generationFailure != null) generationFailure.addSuppressed(cleanupFailure);
                                else throw cleanupFailure;
                            }
                    }
                });
    }

    public ImageSuperResolutionResult upscaleImage(EngineId selected, ImageSuperResolutionRequest request,
                                                   ExecutionContext context)
            throws IOException, InterruptedException {
        ImageSuperResolutionEngine engine = select(platform.imageSuperResolutionEngines(), selected);
        return withResources(admission(context, ComputeJobPriority.BACKGROUND,
                        ComputeWorkloadKind.IMAGE_PROCESSING,
                        ComputeResourceDemand.of(ResourceId.MODEL_MEMORY, ResourceId.GPU)), context,
                current -> engine.upscale(request, current));
    }

    public ImageRefinementResult refineImage(EngineId selected, ImageRefinementRequest request,
                                             ExecutionContext context)
            throws IOException, InterruptedException {
        ImageRefinementEngine engine = select(platform.imageRefinementEngines(), selected);
        return withResources(admission(context, ComputeJobPriority.BACKGROUND,
                        ComputeWorkloadKind.IMAGE_PROCESSING,
                        ComputeResourceDemand.of(ResourceId.MODEL_MEMORY, ResourceId.GPU)), context,
                current -> engine.refine(request, current));
    }

    public VideoGenerationResult generateVideo(EngineId selected, VideoGenerationRequest request,
                                               ExecutionContext context) throws IOException, InterruptedException {
        VideoGenerationEngine engine = select(platform.videoGenerationEngines(), selected);
        return withResources(admission(context, ComputeJobPriority.BACKGROUND,
                        ComputeWorkloadKind.IMAGE_PROCESSING,
                        ComputeResourceDemand.of(ResourceId.MODEL_MEMORY, ResourceId.GPU)), context,
                current -> engine.generate(request, current));
    }

    public VideoRenderResult render(EngineId selected, VideoRenderRequest request,
                                    ExecutionContext context) throws IOException, InterruptedException {
        VideoRenderEngine engine = select(platform.videoRenderEngines(), selected);
        return withResources(admission(context, ComputeJobPriority.BACKGROUND,
                        ComputeWorkloadKind.VIDEO_RENDER,
                        videoDemand(request)), context,
                current -> engine.render(request, current));
    }

    public ContentAnalysisResult analyzeContent(EngineId selected,
                                                ContentAnalysisRequest request,
                                                ExecutionContext context)
            throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        ContentAnalysisEngine engine;
        if (selected != null) {
            engine = platform.contentAnalysisEngines().find(selected)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "analysis engine not registered: " + selected));
        } else {
            engine = platform.contentAnalysisEngines().supporting(request.operation()).stream()
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "no local engine registered for " + request.operation()));
        }
        if (!engine.operations().contains(request.operation())) {
            throw new IllegalArgumentException(engine.descriptor().id()
                    + " does not support " + request.operation());
        }
        ComputePreference preference = safeContentAnalysisPreference();
        ComputeResourceDemand resources = contentDemand(
                engine, request.operation(), request.options(),
                request.visualInputs().size(), preference);
        ExecutionContext configured = withPreference(
                context, preference, "content-analysis");
        configured = withVisualDeadline(configured, request.operation(),
                request.options());
        ComputeJobPriority priority = priority(
                request.options().get("computePriority"),
                ComputeJobPriority.INTERACTIVE_ANALYSIS);
        ContentBatchState batch = contentBatch.get();
        if (batch != null) {
            batch.ensureActive();
            configured.cancellation().throwIfCancellationRequested();
            batch.register(engine);
            return engine.analyze(request, batch.contextFor(configured));
        }
        return withResources(admission(configured, priority,
                        request.operation() == ContentAnalysisOperation.MATH_SPEECH
                                ? ComputeWorkloadKind.MATH_SPEECH
                                : ComputeWorkloadKind.CONTENT_ANALYSIS,
                        resources), configured,
                current -> engine.analyze(request, current));
    }

    /**
     * Translates spoken narration without exposing the neutral content-analysis
     * transport contract to reading use cases.
     */
    public String translateNarration(String text, String sourceLanguage,
                                     String targetLanguage,
                                     ExecutionContext context)
            throws IOException, InterruptedException {
        ContentAnalysisRequest request = new ContentAnalysisRequest(
                ContentAnalysisOperation.NARRATION_TRANSLATION, List.of(),
                "Traduce fielmente la narración suministrada. No resumas, no inventes, "
                        + "no agregues explicaciones. Preserva nombres, cifras, variables, "
                        + "relaciones matemáticas y produce texto natural para TTS.",
                text, targetLanguage, "", Map.of(
                "sourceLanguage", sourceLanguage,
                "targetLanguage", targetLanguage,
                "contextWindowTokens", "4096",
                "maxOutputTokens", "768",
                "retryAllowed", "false"));
        String translated = analyzeContent(null, request, context).text().strip();
        if (translated.isBlank()) {
            throw new IOException("La traducción local devolvió texto vacío.");
        }
        return translated;
    }

    /**
     * Runs several independent content requests with one atomic resource lease.
     * Engines that implement {@link ContentAnalysisBatchLifecycle} may keep their
     * model resident until the callback finishes.
     */
    public <T> T withContentAnalysisBatch(
            ExecutionContext context,
            CheckedContentBatch<T> operation)
            throws IOException, InterruptedException {
        return withContentAnalysisBatch(context,
                ContentAnalysisOperation.IMAGE_DESCRIPTION, Map.of(), operation);
    }

    public <T> T withContentAnalysisBatch(
            ExecutionContext context,
            ContentAnalysisOperation representativeOperation,
            Map<String, String> representativeOptions,
            CheckedContentBatch<T> operation)
            throws IOException, InterruptedException {
        Objects.requireNonNull(operation, "operation");
        if (contentBatch.get() != null) {
            return operation.run();
        }
        ComputePreference preference = safeContentAnalysisPreference();
        ExecutionContext configured = withPreference(
                context, preference, "content-analysis-batch");
        ComputeResourceDemand demand = contentDemand(null,
                representativeOperation, representativeOptions,
                1, preference);
        try (ResourceLease lease = scheduler.acquire(admission(
                configured, ComputeJobPriority.INTERACTIVE_ANALYSIS,
                ComputeWorkloadKind.CONTENT_ANALYSIS, demand))) {
            ExecutionContext leased = new ExecutionContext(
                    configured.operationId(), configured.cancellation(),
                    configured.progress(), configured.policy(), lease,
                    configured.staging(), configured.computePreference(),
                    configured.deadline());
            ContentBatchState state = new ContentBatchState(leased);
            contentBatch.set(state);
            try {
                return operation.run();
            } finally {
                contentBatch.remove();
                state.close();
            }
        }
    }

    public MediaEnginePlatform platform() { return platform; }
    public ResourceScheduler resourceScheduler() { return scheduler; }

    private <E extends MediaEngine> E select(EngineRegistry<E> registry, EngineId selected) {
        if (selected != null) return registry.require(selected);
        List<E> engines = registry.engines();
        if (engines.isEmpty()) throw new IllegalStateException("no engine registered for " + registry.capability());
        return engines.getFirst();
    }

    private <T> T withResources(ComputeAdmissionRequest admission,
                                ExecutionContext context,
                                CheckedOperation<T> operation) throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("media-capability") : context;
        try (ResourceLease lease = scheduler.acquire(admission)) {
            return operation.run(new ExecutionContext(current.operationId(), current.cancellation(), current.progress(),
                    current.policy(), lease, current.staging(), current.computePreference(),
                    current.deadline()));
        }
    }

    private ComputePreference safeContentAnalysisPreference() {
        return safePreference(contentAnalysisComputePreference,
                ComputePreference.automatic());
    }

    private static ComputePreference safePreference(
            Supplier<ComputePreference> supplier,
            ComputePreference fallback) {
        try {
            return Objects.requireNonNullElse(supplier.get(), fallback);
        } catch (RuntimeException failure) {
            return fallback;
        }
    }

    private static ComputeAdmissionRequest admission(
            ExecutionContext context,
            ComputeJobPriority priority,
            ComputeWorkloadKind workload,
            ComputeResourceDemand demand) {
        ExecutionContext current = context == null
                ? ExecutionContext.defaults("media-capability") : context;
        return new ComputeAdmissionRequest(
                current.operationId() + "-" + java.util.UUID.randomUUID(),
                current.operationId(), priority, workload, demand,
                current.cancellation(), current.deadline());
    }

    private static ComputePreference withGpuPermission(
            ComputePreference configured, boolean allowGpu) {
        if (allowGpu) return configured;
        return ComputePreference.cpuOnly(configured.allowHostMemory());
    }

    private static ExecutionContext withPreference(
            ExecutionContext context,
            ComputePreference preference,
            String fallbackOperationId) {
        ExecutionContext requested = context == null
                ? ExecutionContext.defaults(fallbackOperationId) : context;
        return new ExecutionContext(
                requested.operationId(), requested.cancellation(), requested.progress(),
                requested.policy(), requested.resourceLease(), requested.staging(), preference,
                requested.deadline());
    }

    private static ExecutionContext withVisualDeadline(
            ExecutionContext context, ContentAnalysisOperation operation,
            Map<String, String> options) {
        ExecutionContext current = context == null
                ? ExecutionContext.defaults("content-analysis") : context;
        if (current.deadline().bounded()) {
            return current;
        }
        if (operation != ContentAnalysisOperation.PAGE_SEMANTIC_READING
                && operation != ContentAnalysisOperation.NARRATION_TRANSLATION) {
            return current;
        }
        long milliseconds = operation == ContentAnalysisOperation.NARRATION_TRANSLATION
                ? 5L * 60L * 1000L : 20L * 60L * 1000L;
        try {
            String configured = options == null ? ""
                    : options.getOrDefault("absoluteTimeoutMs", "");
            if (!configured.isBlank()) milliseconds = Long.parseLong(configured);
        } catch (NumberFormatException ignored) {
            // Keep the production default when test/adapter input is invalid.
        }
        return current.withDeadline(OperationDeadline.after(
                Duration.ofMillis(Math.max(1L, milliseconds))));
    }

    private static ComputeResourceDemand contentDemand(
            ContentAnalysisEngine engine,
            ContentAnalysisOperation operation,
            Map<String, String> options,
            int visualInputs,
            ComputePreference preference) {
        if (operation == ContentAnalysisOperation.MATH_SPEECH) {
            return ComputeResourceDemand.NONE;
        }
        final long MIB = 1024L * 1024L;
        ComputeDeviceId device = device(preference);
        String engineId = engine == null ? "qwen3-vl-local"
                : engine.descriptor().id().value();
        String model = options == null ? "qwen3-vl:4b-instruct-q8_0"
                : options.getOrDefault("model", "qwen3-vl:4b-instruct-q8_0");
        int contextTokens = integer(options, "contextWindowTokens",
                operation == ContentAnalysisOperation.PAGE_SEMANTIC_READING
                        ? 8192 : 4096);
        int batch = integer(options, "batchSize", 512);
        String kvCache = options == null ? "q8_0"
                : options.getOrDefault("kvCacheType", "q8_0");
        boolean flash = options == null || Boolean.parseBoolean(
                options.getOrDefault("flashAttention", "true"));
        long modelHostMib = integer(options, "modelHostMib",
                model.contains(":8b-") ? 7_475 : 4_000);
        long modelVramMib = integer(options, "modelVramMib",
                model.contains(":8b-") ? 2_300 : 1_800);
        long marginalHostMib = integer(options, "marginalHostMib",
                model.contains(":8b-") ? 640 : 512);
        long marginalVramMib = integer(options, "marginalVramMib",
                model.contains(":8b-") ? 512 : 384);
        String profile = model + "-ctx" + contextTokens + "-batch" + batch
                + "-kv" + kvCache + "-flash" + flash;
        if (preference.mode() == ComputePreference.Mode.CPU_ONLY) {
            ModelResidencyDemand residency = new ModelResidencyDemand(
                    new ModelResidencyKey(engineId, model, profile, device),
                    modelHostMib * MIB, 0L);
            return new ComputeResourceDemand(Map.of(
                    ResourceId.QWEN_INFERENCE, 1,
                    ResourceId.CPU_HEAVY, 1),
                    Math.max(marginalHost(contextTokens, visualInputs),
                            marginalHostMib * MIB), 0L,
                    preference.allowHostMemory(), device, 0,
                    residency, EncoderResourceDemand.none());
        }
        ModelResidencyDemand residency = new ModelResidencyDemand(
                new ModelResidencyKey(engineId, model, profile, device),
                modelHostMib * MIB, modelVramMib * MIB);
        return new ComputeResourceDemand(Map.of(
                ResourceId.QWEN_INFERENCE, 1,
                ResourceId.CPU_HEAVY, 1),
                Math.max(marginalHost(contextTokens, visualInputs),
                        marginalHostMib * MIB),
                marginalVramMib * MIB + Math.max(0, visualInputs - 1) * 96L * MIB,
                preference.allowHostMemory(), device, 1,
                residency, EncoderResourceDemand.none());
    }

    private static ComputeResourceDemand videoDemand(VideoRenderRequest request) {
        final long MIB = 1024L * 1024L;
        VideoEncoderKind kind = request.effectivePlan()
                .encodingPreference().exactKind();
        if (!kind.hardware()) {
            return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                    768L * MIB, 0L, true, ComputeDeviceId.CPU_0,
                    0, null, EncoderResourceDemand.none());
        }
        String requestedDevice = request.options().getOrDefault("computeDeviceId", "");
        ComputeDeviceId device = requestedDevice.isBlank()
                ? ComputeDeviceId.gpu(kind.vendor(), 0)
                : ComputeDeviceId.parse(requestedDevice);
        return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                384L * MIB, 192L * MIB, true, device, 0, null,
                new EncoderResourceDemand(kind, device, 1));
    }

    private static ComputeDeviceId device(ComputePreference preference) {
        if (preference.mode() == ComputePreference.Mode.CPU_ONLY) {
            return ComputeDeviceId.CPU_0;
        }
        return preference.mode() == ComputePreference.Mode.SPECIFIC_DEVICE
                ? ComputeDeviceId.parse(preference.deviceId())
                : ComputeDeviceId.AUTO_GPU_0;
    }

    private static long marginalHost(int contextTokens, int visualInputs) {
        final long MIB = 1024L * 1024L;
        long context = Math.max(128L * MIB,
                Math.min(1_024L * MIB, (long) contextTokens * 64L * 1024L));
        return context + Math.max(1, visualInputs) * 128L * MIB;
    }

    private static int integer(Map<String, String> values,
                               String key, int fallback) {
        try {
            return values == null ? fallback
                    : Integer.parseInt(values.getOrDefault(key,
                    Integer.toString(fallback)));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static ComputeJobPriority priority(
            String value, ComputeJobPriority fallback) {
        try {
            return value == null || value.isBlank()
                    ? fallback
                    : ComputeJobPriority.valueOf(value.strip().toUpperCase(
                    java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return fallback;
        }
    }

    @FunctionalInterface private interface CheckedOperation<T> {
        T run(ExecutionContext context) throws IOException, InterruptedException;
    }

    @FunctionalInterface
    public interface CheckedContentBatch<T> {
        T run() throws IOException, InterruptedException;
    }

    private static final class ContentBatchState {
        private final ExecutionContext context;
        private final LinkedHashMap<Object, ContentAnalysisBatchLifecycle>
                lifecycles = new LinkedHashMap<>();

        private ContentBatchState(ExecutionContext context) {
            this.context = context;
        }

        private ExecutionContext context() {
            return context;
        }

        private ExecutionContext contextFor(ExecutionContext request) {
            return new ExecutionContext(
                    request.operationId(), request.cancellation(),
                    request.progress(), request.policy(),
                    context.resourceLease(), request.staging(),
                    context.computePreference(), context.deadline().bounded()
                            ? context.deadline() : request.deadline());
        }

        private void ensureActive() throws InterruptedException {
            context.cancellation().throwIfCancellationRequested();
        }

        private void register(ContentAnalysisEngine engine)
                throws IOException, InterruptedException {
            if (!(engine instanceof ContentAnalysisBatchLifecycle lifecycle)) {
                return;
            }
            Object identity = lifecycle.contentAnalysisBatchIdentity();
            if (lifecycles.containsKey(identity)) return;
            lifecycle.beginContentAnalysisBatch(context);
            lifecycles.put(identity, lifecycle);
        }

        private void close() throws IOException, InterruptedException {
            IOException ioFailure = null;
            InterruptedException interrupted = null;
            var values = new java.util.ArrayList<>(lifecycles.values());
            java.util.Collections.reverse(values);
            for (ContentAnalysisBatchLifecycle lifecycle : values) {
                try {
                    lifecycle.endContentAnalysisBatch(context);
                } catch (IOException failure) {
                    if (ioFailure == null) ioFailure = failure;
                    else ioFailure.addSuppressed(failure);
                } catch (InterruptedException failure) {
                    if (interrupted == null) interrupted = failure;
                    else interrupted.addSuppressed(failure);
                }
            }
            if (interrupted != null) throw interrupted;
            if (ioFailure != null) throw ioFailure;
        }
    }
}
