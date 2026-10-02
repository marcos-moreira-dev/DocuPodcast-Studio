package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrationRenderPlanUseCaseTest {
    @Test
    void theatreSynthesizesACompleteInterventionAsOneUnit() {
        NarrationSegment intervention = new NarrationSegment(
                "SEG-INTERVENCION-17", NarrationSegmentType.PARAGRAPH,
                "Concha", "En el pan. En la casa. En el trabajo.", List.of("B0017"),
                "CHR-CONCHA", "VOC-CONCHA", "STY-ANGRY", Map.of());
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Obra", "es", "obra.md", List.of(intervention));
        DocuPodcastProject theatre = DocuPodcastProject.createNew(
                "Obra", ProjectMode.THEATRE_PRODUCTION);

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase().build(script, theatre);

        assertEquals(1, plan.unitCount());
        assertEquals("SEG-INTERVENCION-17-U001", plan.units().getFirst().id());
        assertEquals(intervention.narrationText(), plan.units().getFirst().text());
        assertEquals("STY-ANGRY", plan.units().getFirst().performanceStyleId());
    }

    @Test
    void theatreImportedAudioAlsoUsesTheCompleteInterventionWithoutManualCues() {
        NarrationSegment intervention = new NarrationSegment(
                "SEG-INTERVENCION-18", NarrationSegmentType.PARAGRAPH,
                "Concha", "Primera oración. Segunda oración.", List.of("B0018"),
                "CHR-CONCHA", "VOC-CONCHA", "STY-NEUTRAL", Map.of());
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Obra", "es", "obra.md", List.of(intervention));
        NarrativeLayerAssignment importedAudio = new NarrativeLayerAssignment(
                "LAYER-AUDIO-18", NarrativeLayerKind.HUMAN_AUDIO,
                new ScriptTextRange(intervention.id(), 0, intervention.narrationText().length()),
                "AUD-INTERVENCION-18", "Interpretación importada", "");
        DocuPodcastProject theatre = DocuPodcastProject.createNew(
                        "Obra", ProjectMode.THEATRE_PRODUCTION)
                .withNarrativeLayerAssignment(importedAudio);

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase().build(script, theatre);

        assertEquals(1, plan.unitCount());
        assertTrue(plan.units().getFirst().usesAudioClip());
        assertEquals("AUD-INTERVENCION-18", plan.units().getFirst().audioAssetId());
        assertEquals(intervention.narrationText(), plan.units().getFirst().text());
    }

    @Test
    void keepsLogicalWordTitlesOutOfTheAudioPlan() {
        NarrationSegment title = new NarrationSegment(
                "SEG-001", NarrationSegmentType.HEADING, "Mapa de batalla",
                "0.2 Mapa de batalla", List.of("B0001"), "CHR-NARRATOR",
                "VOC-NARRATOR", "STY-NEUTRAL", Map.of("logicalOnly", "true"));
        NarrationSegment paragraph = NarrationSegment.of(
                "SEG-002", NarrationSegmentType.PARAGRAPH, "Contenido",
                "Esta oración sí debe narrarse.", List.of("B0002"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Clase", "es", "clase.docx", List.of(title, paragraph));

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase().build(script, List.of());

        assertEquals(1, plan.unitCount());
        assertEquals("SEG-002-U001", plan.units().getFirst().id());
    }

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

        NarrativeLayerAssignment fragmentVoice = new NarrativeLayerAssignment(
                "LAYER-FRAGMENT-VOICE", NarrativeLayerKind.VOICE,
                new ScriptTextRange("SEG-001", 0, segment.narrationText().length()),
                "VOC-FRAGMENT", "Voz específica", "");
        NarrationRenderPlan overridden = new BuildNarrationRenderPlanUseCase().build(
                script, project.withNarrativeLayerAssignment(fragmentVoice));

        assertEquals("VOC-FRAGMENT", overridden.units().getFirst().voiceProfileId());
    }

    @Test
    void assignsStageDirectionsAnUnusedNarrativeVoice() {
        NarrationSegment direction = new NarrationSegment(
                "SEG-ACOTACION-001", NarrationSegmentType.PARAGRAPH,
                "Acotación", "La plaza queda vacía.", List.of("B0001"),
                "CHR-ACOTACION", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("theatreStageDirection", "true", "logicalOnly", "true"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Obra", "es", "obra.md", List.of(direction));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(), List.of(), List.of(
                new TheatreProjectLayer.VoiceRoleAlias(
                        "VOICE-ACOTACION", "ACOTACION", "VOC-NARRATOR",
                        "CHR-ACOTACION", "Alias heredado"),
                new TheatreProjectLayer.VoiceRoleAlias(
                        "VOICE-ACTOR", "ACTOR",
                        "VOC-PRESET-HOMBRE-MADURO-NARRATIVO",
                        "CHR-ACTOR", "Voz de personaje")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra")
                .withTheatre(theatre);

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase()
                .build(script, project);

        assertEquals(1, plan.unitCount());
        assertEquals("VOC-PRESET-MUJER-ADULTA-CALIDA-NARRATIVA",
                plan.units().getFirst().voiceProfileId());
        assertEquals("STY-NEUTRAL", plan.units().getFirst().performanceStyleId());
    }

    @Test
    void projectDefaultVoiceReplacesOnlyTheBuiltInNarrator() {
        NarrationSegment narrator = NarrationSegment.of(
                "SEG-001", NarrationSegmentType.PARAGRAPH, "Inicio",
                "Texto del narrador.", List.of("B0001"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Lectura", "es", "lectura.docx", List.of(narrator));
        DocuPodcastProject project = DocuPodcastProject.createNew("Lectura")
                .withDocumentDefaultVoiceProfileId("VOC-MARIA");

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase().build(script, project);

        assertEquals("VOC-MARIA", plan.units().getFirst().voiceProfileId());

        NarrativeLayerAssignment fragmentVoice = new NarrativeLayerAssignment(
                "LAYER-FRAGMENT", NarrativeLayerKind.VOICE,
                new ScriptTextRange("SEG-001", 0, narrator.narrationText().length()),
                "VOC-PEDRO", "Voz del fragmento", "");
        NarrationRenderPlan overridden = new BuildNarrationRenderPlanUseCase().build(
                script, project.withNarrativeLayerAssignment(fragmentVoice));

        assertEquals("VOC-PEDRO", overridden.units().getFirst().voiceProfileId());
    }

    @Test
    void projectDefaultToneAppliesOnlyWithoutSpecificEmotionOverride() {
        NarrationSegment narrator = NarrationSegment.of(
                "SEG-001", NarrationSegmentType.PARAGRAPH, "Inicio",
                "Texto con tono global.", List.of("B0001"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Lectura", "es", "lectura.docx", List.of(narrator));
        DocuPodcastProject project = DocuPodcastProject.createNew("Lectura")
                .withDocumentDefaultVoiceProfileId("VOC-MARIA")
                .withDocumentDefaultVoiceToneId("STY-FELIZ");

        NarrationRenderPlan plan = new BuildNarrationRenderPlanUseCase()
                .build(script, project);
        assertEquals("VOC-MARIA", plan.units().getFirst().voiceProfileId());
        assertEquals("STY-FELIZ", plan.units().getFirst().performanceStyleId());

        NarrativeLayerAssignment emotion = new NarrativeLayerAssignment(
                "LAYER-EMOTION", NarrativeLayerKind.EMOTION,
                new ScriptTextRange("SEG-001", 0, narrator.narrationText().length()),
                "STY-TRISTE", "Triste", "");
        NarrationRenderPlan overridden = new BuildNarrationRenderPlanUseCase()
                .build(script, project.withNarrativeLayerAssignment(emotion));

        assertEquals("STY-TRISTE",
                overridden.units().getFirst().performanceStyleId());
    }
}
