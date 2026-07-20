package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** Narrow synchronous application facade over neutral engines and the shared resource scheduler. */
public final class MediaCapabilityService {
    private final MediaEnginePlatform platform;
    private final ResourceScheduler scheduler;

    public MediaCapabilityService(MediaEnginePlatform platform, ResourceScheduler scheduler) {
        this.platform = Objects.requireNonNull(platform, "media engine platform");
        this.scheduler = Objects.requireNonNull(scheduler, "resource scheduler");
    }

    public VoiceSynthesisBatchResult synthesize(EngineId selected, VoiceSynthesisBatchRequest request,
                                                ExecutionContext context) throws IOException, InterruptedException {
        VoiceSynthesisEngine engine = select(platform.voiceEngines(), selected);
        return withResources(ResourceRequirement.of(ResourceId.MODEL_MEMORY, ResourceId.CPU_HEAVY), context,
                current -> engine.synthesizeBatch(request, current));
    }

    public ImageGenerationResult generateImage(EngineId selected, ImageGenerationRequest request,
                                               ExecutionContext context) throws IOException, InterruptedException {
        ImageGenerationEngine engine = select(platform.imageEngines(), selected);
        return withResources(ResourceRequirement.of(ResourceId.MODEL_MEMORY, ResourceId.GPU), context,
                current -> engine.generate(request, current));
    }

    public VideoGenerationResult generateVideo(EngineId selected, VideoGenerationRequest request,
                                               ExecutionContext context) throws IOException, InterruptedException {
        VideoGenerationEngine engine = select(platform.videoGenerationEngines(), selected);
        return withResources(ResourceRequirement.of(ResourceId.MODEL_MEMORY, ResourceId.GPU), context,
                current -> engine.generate(request, current));
    }

    public VideoRenderResult render(EngineId selected, VideoRenderRequest request,
                                    ExecutionContext context) throws IOException, InterruptedException {
        VideoRenderEngine engine = select(platform.videoRenderEngines(), selected);
        return withResources(ResourceRequirement.of(ResourceId.CPU_HEAVY, ResourceId.VIDEO_ENCODER), context,
                current -> engine.render(request, current));
    }

    public MediaEnginePlatform platform() { return platform; }
    public ResourceScheduler resourceScheduler() { return scheduler; }

    private <E extends MediaEngine> E select(EngineRegistry<E> registry, EngineId selected) {
        if (selected != null) return registry.require(selected);
        List<E> engines = registry.engines();
        if (engines.isEmpty()) throw new IllegalStateException("no engine registered for " + registry.capability());
        return engines.getFirst();
    }

    private <T> T withResources(ResourceRequirement requirement, ExecutionContext context,
                                CheckedOperation<T> operation) throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("media-capability") : context;
        try (ResourceLease lease = scheduler.acquire(requirement, current.cancellation())) {
            return operation.run(new ExecutionContext(current.operationId(), current.cancellation(), current.progress(),
                    current.policy(), lease, current.staging()));
        }
    }

    @FunctionalInterface private interface CheckedOperation<T> {
        T run(ExecutionContext context) throws IOException, InterruptedException;
    }
}
