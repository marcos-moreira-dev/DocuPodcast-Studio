package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrationRenderPlanUseCaseTest {
    @Test
    void buildsSentenceUnitsAndResolvesEffectiveLayers() {
        NarrationSegment segment = NarrationSegment.of(
                "SEG-001",
                NarrationSegmentType.PARAGRAPH,
                "Geología",
                "La geología estudia la Tierra. Los volcanes liberan energía.",
                List.of("B0001")
        );
        NarrationScriptDocument script = NarrationScriptDocument.create("Clase", "es", "geologia.docx", List.of(segment));
        List<NarrativeLayerAssignment> layers = List.of(
                new NarrativeLayerAssignment("LAYER-VOICE", NarrativeLayerKind.VOICE,
                        new ScriptTextRange("SEG-001", 0, 31), "VOC-PROFESOR", "Profesor", ""),
                new NarrativeLayerAssignment("LAYER-EMOTION", NarrativeLayerKind.EMOTION,
                        new ScriptTextRange("SEG-001", 0, 31), "STY-DIDACTIC", "Didáctico", ""),
                new NarrativeLayerAssignment("LAYER-IMAGE", NarrativeLayerKind.IMAGE,
                        new ScriptTextRange("SEG-001", 32, 61), new DocumentTextRange("B0001", 32, 61),
                        "IMG-VOLCAN", "Volcán", ""),
                new NarrativeLayerAssignment("LAYER-AUDIO", NarrativeLayerKind.HUMAN_AUDIO,
                        new ScriptTextRange("SEG-001", 32, 61), new DocumentTextRange("B0001", 32, 61),
                        "AUD-PAJAROS", "Clip externo", "Audio genérico elegido por el usuario")
        );

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase().build(script, layers);

        assertEquals(2, plan.unitCount());
        NarrationRenderUnit first = plan.unitById("SEG-001-U001").orElseThrow();
        assertTrue(first.usesTts());
        assertEquals("VOC-PROFESOR", first.voiceProfileId());
        assertEquals("STY-DIDACTIC", first.performanceStyleId());
        NarrationRenderUnit second = plan.unitById("SEG-001-U002").orElseThrow();
        assertTrue(second.usesAudioClip());
        assertEquals("AUD-PAJAROS", second.audioAssetId());
        assertEquals("IMG-VOLCAN", second.imageAssetId());
        assertTrue(second.hasDocumentRange());
    }

    @Test
    void fallsBackToSegmentVoiceAndStyleWhenNoLayerOverridesExist() {
        NarrationSegment segment = new NarrationSegment(
                "SEG-001",
                NarrationSegmentType.PARAGRAPH,
                "Intro",
                "Una sola oración sin capas.",
                List.of("B0001"),
                "CHR-NARRATOR",
                "VOC-BASE",
                "STY-BASE",
                java.util.Map.of()
        );
        NarrationScriptDocument script = NarrationScriptDocument.create("Clase", "es", "clase.docx", List.of(segment));

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase().build(script, List.of());

        NarrationRenderUnit unit = plan.units().getFirst();
        assertEquals("VOC-BASE", unit.voiceProfileId());
        assertEquals("STY-BASE", unit.performanceStyleId());
        assertTrue(unit.usesTts());
    }

    @Test
    void usesTheatreCharacterVoiceAliasWhenRenderingCharacterCue() {
        NarrationSegment segment = new NarrationSegment(
                "SEG-001",
                NarrationSegmentType.PARAGRAPH,
                "Bigote",
                "CAPITAN BIGOTE: Teniente, revise el combustible.",
                List.of("B0001"),
                "CHR-CAPITAN-BIGOTE",
                "VOC-NARRATOR",
                "STY-BASE",
                java.util.Map.of()
        );
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(segment));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(),
                List.of(),
                List.of(new TheatreProjectLayer.VoiceRoleAlias(
                        "VOICE-DEMO-BIGOTE",
                        "CAPITAN BIGOTE",
                        "VOC-DEMO-BIGOTE",
                        "CHR-CAPITAN-BIGOTE",
                        "Asignada por ficha de personaje.")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra").withTheatre(theatre);

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase().build(script, project);

        assertEquals("VOC-DEMO-BIGOTE", plan.units().getFirst().voiceProfileId());
    }
}
