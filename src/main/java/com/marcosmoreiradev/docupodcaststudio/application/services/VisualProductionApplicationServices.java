package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.visual.BuildVisualProductionProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.visual.BuildVisualPromptContextUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualAssetTracePolicy;

/** Transversal visual production services over FragmentId. */
public record VisualProductionApplicationServices(
        BuildVisualProductionProjectionUseCase buildVisualProductionProjection,
        BuildVisualPromptContextUseCase buildVisualPromptContext,
        VisualAssetTracePolicy visualAssetTracePolicy
) {
    public static VisualProductionApplicationServices defaults() {
        VisualAssetTracePolicy tracePolicy = new VisualAssetTracePolicy();
        return new VisualProductionApplicationServices(
                new BuildVisualProductionProjectionUseCase(tracePolicy),
                new BuildVisualPromptContextUseCase(),
                tracePolicy);
    }
}
