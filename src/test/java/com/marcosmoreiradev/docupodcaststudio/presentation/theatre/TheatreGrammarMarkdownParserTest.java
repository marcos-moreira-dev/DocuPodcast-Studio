package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan.ImportPlan;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreGrammarMarkdownParserTest {
    @Test
    void parsesStageDirectionsAsNarratedNeutralInterventions() {
        ImportPlan plan = TheatreGrammarMarkdownParser.parse("""
                # Obra
                ## Acto: Primero
                ### Escena: Plaza
                ACOTACIÓN: Mientras Concha bailaba, Eloy seguía trabajando.
                """);

        var direction = plan.interventions().getFirst();
        assertTrue(direction.stageDirection());
        assertEquals("NEUTRAL", direction.tono());
        assertEquals("Mientras Concha bailaba, Eloy seguía trabajando.", direction.spokenText());
        var document = new com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarDocumentBuilder()
                .build(plan, Path.of("obra.teatro.md"));
        var segment = com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarDocumentBuilder
                .segment(document.blocks().getFirst());
        assertTrue(segment.narratable());
        assertEquals("true", segment.metadata().get("theatreStageDirection"));
    }

    @Test
    void parsesSceneTextBoundsSpatialMapAndInterventionMetadata() {
        ImportPlan plan = TheatreGrammarMarkdownParser.parse("""
                # Obra

                - Personaje: CAPITAN BIGOTE | voz=VOC-1 | tono=SERIOUS | nota=Piloto

                ## Acto: El vuelo

                ### Escena: El hangar
                > texto_inicio=5 | texto_fin=10 | mapa_espacial=fragmentos/mapa.png | fondo_escenario=fondos/hangar.png

                CAPITAN BIGOTE: Revise el combustible.
                > origen=fondo centro | destino=centro derecha | imagen=fragmentos/uno.png | tono=DRAMATIC | plano=CERCA_CENTRO_NIVEL | aplicar_plano=false | fondo=fondos/hangar.png | contexto_ia=Camara fija y telon teatral.
                """);

        var scene = plan.acts().get(0).scenes().get(0);
        var intervention = plan.interventions().get(0);

        assertEquals("El hangar", scene.name());
        assertEquals(5, scene.textStartIndex());
        assertEquals(10, scene.textEndIndex());
        assertEquals("fragmentos/mapa.png", scene.spatialMap());
        assertEquals("fondos/hangar.png", scene.stageBackdrop());
        assertEquals("CAPITAN BIGOTE", intervention.characterName());
        assertEquals("fondo centro", intervention.origin());
        assertEquals("centro derecha", intervention.destination());
        assertEquals("fragmentos/uno.png", intervention.images().get(0));
        assertEquals("DRAMATIC", intervention.tono());
        assertEquals("CERCA_CENTRO_NIVEL", intervention.cameraCue());
        assertFalse(intervention.applyCamera());
        assertEquals("fondos/hangar.png", intervention.stageBackdrop());
        assertEquals("Camara fija y telon teatral.", intervention.aiContextText());
    }

    @Test
    void bundledAviadoresManifestDeclaresDocxSceneRanges() throws Exception {
        ImportPlan plan = TheatreGrammarMarkdownParser.parse(Files.readString(
                Path.of("src/main/resources/examples/aviadores-comicos/PROYECTO_DEMO.md")));

        var scenes = plan.acts().get(0).scenes();

        assertEquals(3, scenes.size());
        assertEquals(3, plan.characters().size());
        assertEquals("personajes/narrador/narrador_01_frontal.png", plan.characters().get(0).imagenes().get(0).path());
        assertEquals("", plan.characters().get(0).imagenes().get(0).sceneId());
        assertEquals(9, plan.objects().size());
        assertEquals(5, scenes.get(0).textStartIndex());
        assertEquals(10, scenes.get(0).textEndIndex());
        assertEquals(12, scenes.get(1).textStartIndex());
        assertEquals(20, scenes.get(1).textEndIndex());
        assertEquals(22, scenes.get(2).textStartIndex());
        assertEquals(27, scenes.get(2).textEndIndex());
        assertEquals("mapas/mapa-espacial.png", scenes.get(0).spatialMap());
        assertEquals("mapas/mapa-espacial.png", scenes.get(1).spatialMap());
        assertEquals("mapas/mapa-espacial.png", scenes.get(2).spatialMap());
        assertEquals(21, plan.interventions().size());
    }

    @Test
    void exportedTemplateDeclaresCurrentToneCatalogAndParseableInteractionMetadata() {
        String template = TheatreGrammarTemplate.markdown();
        ImportPlan plan = TheatreGrammarMarkdownParser.parse(template);

        assertTrue(template.contains("<!-- TONO_CATALOGO:"));
        assertTrue(template.contains("NEUTRAL"));
        assertTrue(template.contains("HEROIC"));
        assertTrue(template.contains("SEDUCTIVE_NON_EXPLICIT"));
        assertTrue(template.contains("interaccion=TENIENTE TORNILLO, Publico"));
        assertTrue(template.contains("plano=CERCA_CENTRO_NIVEL"));
        assertTrue(template.contains("aplicar_plano=false"));
        assertTrue(template.contains("fondo_escenario="));
        assertTrue(template.contains("quitar_fondo=true"));
        assertTrue(template.contains("contexto_ia="));
        var capitan = plan.interventions().stream()
                .filter(intervention -> intervention.characterName().equals("CAPITAN BIGOTE"))
                .findFirst()
                .orElseThrow();
        assertFalse(plan.toneCatalog().isBlank());
        assertEquals("TENIENTE TORNILLO, Publico", capitan.interactionTarget());
        assertEquals("SERIOUS", capitan.tono());
        assertEquals("CERCA_DERECHA_NIVEL", capitan.cameraCue());
        assertEquals("assets/fondos/hangar-amanecer.png", capitan.stageBackdrop());
        assertTrue(plan.interventions().stream().noneMatch(intervention -> intervention.characterName().contains("| tono=")));
    }
}
