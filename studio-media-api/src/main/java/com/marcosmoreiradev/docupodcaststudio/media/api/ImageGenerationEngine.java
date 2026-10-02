package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

public interface ImageGenerationEngine extends MediaEngine {
    /** Unknown third-party engines retain exclusive admission until they declare a physical demand. */
    default ComputeResourceDemand resourceDemand(ImageGenerationRequest request, ComputePreference preference) {
        return ComputeResourceDemand.of(ResourceId.MODEL_MEMORY, ResourceId.GPU);
    }

    /** Called while the lease is held, when orchestration must make room for another model. */
    default void releaseIdleResources(ExecutionContext context) throws IOException, InterruptedException { }

    ImageGenerationResult generate(ImageGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}
