package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioGenerationRequestRenderPlanTest {
    @TempDir
    Path tempDir;

    @Test
    void usesOnlyRenderUnitsThatRequireTtsGeneration() {
        NarrationScriptDocument script = script();
        RenderUnit ttsUnit = new RenderUnit("SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 18), null,
                "Aviador", "Texto para generar", RenderUnitKind.SPOKEN_ONLY,
                "VOC-PILOT", "STY-COMIC", "", "", 5.0, List.of("LAYER-VOICE"));
        RenderUnit externalAudio = new RenderUnit("SEG-001-U002", "SEG-001-U002", "SEG-001", 1,
                new ScriptTextRange("SEG-001", 19, 38), null,
                "Radio", "Audio externo", RenderUnitKind.SPOKEN_ONLY,
                "", "", "AUD-RADIO", "", 5.0, List.of("LAYER-AUDIO"));
        RenderUnit visualSilent = RenderUnit.visualSilent("VISUAL-B0002", 2,
                new DocumentTextRange("B0002", 0, 0), "Tabla", "IMG-TABLA", 5.0, List.of("LAYER-IMG"));
        RenderUnitPlan plan = new RenderUnitPlan("UNITPLAN-SCRIPT-001", script.id(),
                List.of(ttsUnit, externalAudio, visualSilent), 5.0, Instant.EPOCH);

        AudioGenerationRequest request = new AudioGenerationRequest(script, plan, tempDir, "Job TI2");

        assertTrue(request.usesRenderUnitPlan());
        assertEquals(1, request.generationUnitCount());
        AudioGenerationUnit unit = request.generationUnits().getFirst();
        assertEquals("SEG-001-U001", unit.id());
        assertEquals("SEG-001", unit.sourceSegmentId());
        assertEquals("VOC-PILOT", unit.voiceProfileId());
        assertEquals("STY-COMIC", unit.performanceStyleId());
    }

    @Test
    void keepsLegacySegmentFallbackWhenNoRenderPlanIsProvided() {
        AudioGenerationRequest request = new AudioGenerationRequest(script(), tempDir, "Legacy");

        assertEquals(1, request.generationUnitCount());
        assertEquals("SEG-001", request.generationUnits().getFirst().id());
    }

    @Test
    void renderUnitGenerationUsesColonCleanedSegmentTextWhenProjectionWasPreparedThatWay() {
        NarrationSegment cleaned = NarrationSegment.of(
                "SEG-001",
                NarrationSegmentType.PARAGRAPH,
                "Dialogo",
                "Teniente, revise el combustible.",
                List.of("B0001"));
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "doc", List.of(cleaned));
        RenderUnit staleUnit = new RenderUnit("SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 43), null,
                "Dialogo", "CAPITAN BIGOTE: Teniente, revise el combustible.", RenderUnitKind.SPOKEN_ONLY,
                "VOC-PILOT", "STY-COMIC", "", "", 5.0, List.of());
        RenderUnitPlan plan = new RenderUnitPlan("UNITPLAN-SCRIPT-001", script.id(),
                List.of(staleUnit), 5.0, Instant.EPOCH);

        AudioGenerationRequest request = new AudioGenerationRequest(script, plan, tempDir, "Job colon");

        assertEquals("Teniente, revise el combustible.", request.generationUnits().getFirst().text());
    }

    @Test
    void renderUnitGenerationKeepsColonPrefixWhenProjectionWasPreparedWithFullText() {
        NarrationSegment full = NarrationSegment.of(
                "SEG-001",
                NarrationSegmentType.PARAGRAPH,
                "Dialogo",
                "CAPITAN BIGOTE: Teniente, revise el combustible.",
                List.of("B0001"));
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "doc", List.of(full));
        RenderUnit unit = new RenderUnit("SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 43), null,
                "Dialogo", "CAPITAN BIGOTE: Teniente, revise el combustible.", RenderUnitKind.SPOKEN_ONLY,
                "VOC-PILOT", "STY-COMIC", "", "", 5.0, List.of());
        RenderUnitPlan plan = new RenderUnitPlan("UNITPLAN-SCRIPT-001", script.id(),
                List.of(unit), 5.0, Instant.EPOCH);

        AudioGenerationRequest request = new AudioGenerationRequest(script, plan, tempDir, "Job colon full");

        assertEquals("CAPITAN BIGOTE: Teniente, revise el combustible.", request.generationUnits().getFirst().text());
    }


    @Test
    void defaultNarratorVoiceUsesConfiguredDocumentVoiceAsFallback() {
        AudioGenerationUnit defaultUnit = new AudioGenerationUnit("SEG-001-U001", "Titulo", "Texto", "SEG-001",
                "VOC-NARRATOR", "", List.of());
        AudioGenerationUnit specificUnit = new AudioGenerationUnit("SEG-001-U002", "Titulo", "Texto", "SEG-001",
                "VOC-MUJER-25", "", List.of());

        assertEquals("VOC-MUJER-25", defaultUnit.effectiveVoiceProfileId("VOC-MUJER-25"));
        assertEquals("VOC-MUJER-25", specificUnit.effectiveVoiceProfileId("VOC-HOMBRE-40"));
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Guion", "es", "doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto para generar. Audio externo.", List.of("B0001"))
        ));
    }
}
