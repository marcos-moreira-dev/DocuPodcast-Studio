package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.render.BuildNarrationRenderPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.render.BuildRenderUnitPlanUseCase;

/** Unit-level narration render plan services. */
public record RenderApplicationServices(
        BuildNarrationRenderPlanUseCase buildNarrationRenderPlan,
        BuildRenderUnitPlanUseCase buildRenderUnitPlan
) {
    public RenderApplicationServices(BuildNarrationRenderPlanUseCase buildNarrationRenderPlan) {
        this(buildNarrationRenderPlan, new BuildRenderUnitPlanUseCase());
    }
}
