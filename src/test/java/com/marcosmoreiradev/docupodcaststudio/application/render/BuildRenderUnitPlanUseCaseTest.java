package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderSourceKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildRenderUnitPlanUseCaseTest {
    @Test
    void convertsNarrationUnitsAndAppendsVisualSilentUnits() {
        NarrationRenderUnit spoken = new NarrationRenderUnit(
                "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 15), null,
                "Texto hablado.", NarrationRenderSourceKind.TEXT_TO_SPEECH,
                "VOC-NARRATOR", "STY-NEUTRAL", "", "", List.of());
        NarrationRenderUnit withImage = new NarrationRenderUnit(
                "SEG-002-U001", "SEG-002", 1,
                new ScriptTextRange("SEG-002", 0, 18), new DocumentTextRange("B0002", 0, 18),
                "Texto con imagen.", NarrationRenderSourceKind.TEXT_TO_SPEECH,
                "VOC-NARRATOR", "STY-NEUTRAL", "", "IMG-001", List.of("LAYER-IMG"));
        NarrationRenderPlan narration = new NarrationRenderPlan("RENDER-SCRIPT-001", "SCRIPT-001",
                List.of(spoken, withImage), Instant.EPOCH);
        RenderUnit visualSilent = RenderUnit.visualSilent("VISUAL-B0003", 2,
                new DocumentTextRange("B0003", 0, 0), "Tabla", "IMG-TABLA", 5.0, List.of("LAYER-TABLA"));

        RenderUnitPlan plan = new BuildRenderUnitPlanUseCase().build(narration, 5.0, List.of(visualSilent));

        assertEquals(3, plan.unitCount());
        assertEquals(2, plan.spokenUnitCount());
        assertEquals(2, plan.visualUnitCount());
        assertEquals(1, plan.silentVisualUnitCount());
        assertEquals(RenderUnitKind.SPOKEN_ONLY, plan.unitById("SEG-001-U001").orElseThrow().kind());
        assertEquals(RenderUnitKind.SPOKEN_WITH_VISUAL, plan.unitById("SEG-002-U001").orElseThrow().kind());
        assertTrue(plan.unitById("VISUAL-B0003").orElseThrow().silentVideoFrame());
    }

    @Test
    void readsSilentDurationFromSettings() {
        NarrationRenderPlan empty = new NarrationRenderPlan("RENDER-SCRIPT-002", "SCRIPT-002", List.of(), Instant.EPOCH);
        OperationalSettings.VideoRenderSettings settings = new OperationalSettings.VideoRenderSettings("", "2K", true, 7.0);

        RenderUnitPlan plan = new BuildRenderUnitPlanUseCase().build(empty, settings);

        assertEquals(7.0, plan.defaultSilentVisualDurationSeconds());
    }
}
