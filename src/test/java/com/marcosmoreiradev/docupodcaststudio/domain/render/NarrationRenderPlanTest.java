package com.marcosmoreiradev.docupodcaststudio.domain.render;

import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrationRenderPlanTest {
    @Test
    void storesUnitLevelNarrationContract() {
        NarrationRenderUnit first = new NarrationRenderUnit(
                "SEG-001-U001",
                "SEG-001",
                0,
                new ScriptTextRange("SEG-001", 0, 12),
                null,
                "Primera frase.",
                NarrationRenderSourceKind.TEXT_TO_SPEECH,
                "VOC-NARRATOR",
                "STY-NEUTRAL",
                "",
                "IMG-001",
                List.of("LAYER-IMG")
        );
        NarrationRenderUnit second = new NarrationRenderUnit(
                "SEG-001-U002",
                "SEG-001",
                1,
                new ScriptTextRange("SEG-001", 13, 25),
                null,
                "Segunda frase.",
                NarrationRenderSourceKind.AUDIO_CLIP,
                "",
                "STY-NEUTRAL",
                "AUD-001",
                "",
                List.of("LAYER-AUD")
        );

        NarrationRenderPlan plan = new NarrationRenderPlan("RENDER-SCRIPT-001", "SCRIPT-001", List.of(first, second), Instant.EPOCH);

        assertEquals(2, plan.unitCount());
        assertEquals(1, plan.ttsUnitCount());
        assertEquals(1, plan.audioClipUnitCount());
        assertEquals(2, plan.unitsForSegment("SEG-001").size());
        assertTrue(plan.unitById("SEG-001-U002").orElseThrow().usesAudioClip());
    }

    @Test
    void rejectsDuplicatedRenderUnits() {
        NarrationRenderUnit first = new NarrationRenderUnit(
                "SEG-001-U001",
                "SEG-001",
                0,
                new ScriptTextRange("SEG-001", 0, 12),
                null,
                "Primera frase.",
                NarrationRenderSourceKind.TEXT_TO_SPEECH,
                "VOC-NARRATOR",
                "STY-NEUTRAL",
                "",
                "",
                List.of()
        );
        assertThrows(IllegalArgumentException.class,
                () -> new NarrationRenderPlan("RENDER-SCRIPT-001", "SCRIPT-001", List.of(first, first), Instant.EPOCH));
    }
}
