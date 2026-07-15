package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds the TI1 cross-media RenderUnit plan from narration units and optional visual-silent units. */
public final class BuildRenderUnitPlanUseCase {
    public static final double DEFAULT_SILENT_VISUAL_SECONDS = 5.0;

    public RenderUnitPlan build(NarrationRenderPlan narrationPlan) {
        return build(narrationPlan, DEFAULT_SILENT_VISUAL_SECONDS, List.of());
    }

    public RenderUnitPlan build(NarrationRenderPlan narrationPlan, OperationalSettings.VideoRenderSettings videoSettings) {
        double duration = videoSettings == null ? DEFAULT_SILENT_VISUAL_SECONDS : videoSettings.silentVisualBlockSeconds();
        return build(narrationPlan, duration, List.of());
    }

    public RenderUnitPlan build(NarrationRenderPlan narrationPlan, double defaultSilentVisualSeconds) {
        return build(narrationPlan, defaultSilentVisualSeconds, List.of());
    }

    public RenderUnitPlan build(
            NarrationRenderPlan narrationPlan,
            double defaultSilentVisualSeconds,
            List<RenderUnit> visualSilentUnits
    ) {
        Objects.requireNonNull(narrationPlan, "narrationPlan");
        ArrayList<RenderUnit> units = new ArrayList<>();
        int index = 0;
        for (var unit : narrationPlan.units()) {
            units.add(RenderUnit.fromNarrationUnit(unit, index++, defaultSilentVisualSeconds));
        }
        if (visualSilentUnits != null) {
            for (RenderUnit visualUnit : visualSilentUnits) {
                if (!visualUnit.kind().silentVisual()) {
                    throw new IllegalArgumentException("Only VISUAL_SILENT units can be appended as visualSilentUnits");
                }
                units.add(visualUnit);
            }
        }
        return new RenderUnitPlan("UNITPLAN-" + narrationPlan.id(), narrationPlan.scriptId(), units,
                defaultSilentVisualSeconds, Instant.now());
    }
}
