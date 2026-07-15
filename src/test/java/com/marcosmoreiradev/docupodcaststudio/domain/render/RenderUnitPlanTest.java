package com.marcosmoreiradev.docupodcaststudio.domain.render;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RenderUnitPlanTest {
    @Test
    void classifiesSpokenAndSilentVisualUnits() {
        RenderUnit spoken = new RenderUnit(
                "RU-001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 20), null,
                "Intro", "Texto narrado", RenderUnitKind.SPOKEN_ONLY,
                "VOC-NARRATOR", "STY-NEUTRAL", "", "", 5.0, List.of());
        RenderUnit visual = RenderUnit.visualSilent(
                "RU-002", 1, new DocumentTextRange("B0002", 0, 0),
                "Tabla de costos", "IMG-TABLA", 5.0, List.of("LAYER-IMG"));

        RenderUnitPlan plan = new RenderUnitPlan("RUP-001", "SCRIPT-001", List.of(spoken, visual), 5.0, Instant.EPOCH);

        assertEquals(2, plan.unitCount());
        assertEquals(1, plan.spokenUnitCount());
        assertEquals(1, plan.visualUnitCount());
        assertEquals(1, plan.silentVisualUnitCount());
        assertEquals(1, plan.omittedFromVideoCount());
        assertTrue(plan.videoUnits().getFirst().silentVideoFrame());
    }

    @Test
    void rejectsInvalidVisualSilentUnitWithoutImage() {
        assertThrows(IllegalArgumentException.class, () -> new RenderUnit(
                "RU-INVALID", "", "", 0, null, new DocumentTextRange("B0001", 0, 0),
                "Imagen", "", RenderUnitKind.VISUAL_SILENT,
                "", "", "", "", 5.0, List.of()));
    }
}
