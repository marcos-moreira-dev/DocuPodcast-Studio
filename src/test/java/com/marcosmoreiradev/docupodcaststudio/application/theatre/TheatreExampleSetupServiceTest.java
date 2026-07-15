package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.TheatreGrammarMarkdownParser;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreExampleSetupServiceTest {
    @Test
    void materializesMarkdownLayerWithoutReplacingPlacementsWithFallback() {
        var plan = TheatreGrammarMarkdownParser.parse("""
                # Obra

                - Personaje: CAPITAN BIGOTE | voz=VOC-PRESET-HOMBRE-60-VETERANO-ECUADOR | tono=SERIOUS | imagen=personajes/capitan.png
                - Objeto: Mapa de ruta | escena=El hangar | imagen=utileria/mapa-ruta.png | nota=Utileria

                ## Acto: El vuelo

                ### Escena: El hangar
                > texto_inicio=5 | texto_fin=10 | mapa_espacial=fragmentos/mapa.png | fondo_escenario=fondos/hangar.png

                CAPITAN BIGOTE: Revise el combustible.
                > origen=fondo centro | destino=centro derecha | imagen=fragmentos/uno.png | plano=CERCA_CENTRO_NIVEL | fondo=fondos/pista.png
                """);

        var result = new TheatreExampleSetupService().execute(
                plan,
                null,
                VoiceLibrary.defaults(),
                Map.of(
                        "mapa.png", "IMG-MAP",
                        "uno.png", "IMG-UNO",
                        "capitan.png", "IMG-CAPITAN",
                        "mapa-ruta.png", "IMG-OBJ-MAPA",
                        "fondos/hangar.png", "IMG-BACKDROP-HANGAR",
                        "fondos/pista.png", "IMG-BACKDROP-PISTA"));

        var scene = result.layer().scenes().get(0);
        var placement = result.layer().textActionPlacements().get(0);

        assertEquals("SCN-EL-HANGAR", scene.id());
        assertEquals("IMG-MAP", scene.spatialMapAssetId());
        assertEquals("SCN-EL-HANGAR", placement.sceneId());
        assertEquals("fondo centro", placement.origin());
        assertEquals("centro derecha", placement.destination());
        assertEquals("Referencia", result.layer().characterImages().get(0).view());
        assertEquals("Referencia", result.layer().objectImages().get(0).view());
        assertEquals("SCN-EL-HANGAR", result.layer().objectImages().get(0).sceneId());
        assertEquals("INTERVENCION-1", result.layer().intervencionesVisuales().get(0).intervencionId());
        assertEquals("IMG-UNO", result.layer().intervencionesVisuales().get(0).assetId());
        assertEquals("CERCA_CENTRO_NIVEL", result.layer().cameraCues().get(0).cameraId());
        assertEquals(2, result.layer().stageBackdrops().size());
        assertEquals(2, result.layer().stageBackdropAssignments().size());
        assertEquals("IMG-BACKDROP-HANGAR", result.layer().stageBackdrops().get(0).assetId());
        assertEquals("IMG-BACKDROP-PISTA", result.layer().stageBackdrops().get(1).assetId());
        assertEquals(5, result.sceneTextRanges().get("SCN-EL-HANGAR").startTextIndex());
        assertEquals(10, result.sceneTextRanges().get("SCN-EL-HANGAR").endTextIndex());
    }

    @Test
    void materializesMultipleInteractionTargetsButOnlyCreatesCharacterActionsForCharacters() {
        var plan = TheatreGrammarMarkdownParser.parse("""
                # Obra

                - Personaje: CAPITAN BIGOTE | voz=VOC-PRESET-HOMBRE-60-VETERANO-ECUADOR
                - Personaje: TENIENTE TORNILLO | voz=VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO
                - Personaje: NARRADOR | voz=VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO

                ## Acto: El vuelo

                ### Escena: El hangar

                CAPITAN BIGOTE: Escuchen todos.
                > origen=centro derecha | destino=centro izquierda | interaccion=TENIENTE TORNILLO, NARRADOR, Publico
                """);

        var result = new TheatreExampleSetupService().execute(
                plan,
                null,
                VoiceLibrary.defaults(),
                Map.of());
        var placement = result.layer().textActionPlacements().get(0);

        assertEquals("TENIENTE TORNILLO, NARRADOR, Publico", placement.interactionTarget());
        assertEquals("centro derecha", placement.characterLocations().get("CAPITAN BIGOTE"));
        assertEquals("centro izquierda", placement.characterLocations().get("TENIENTE TORNILLO"));
        assertEquals("centro izquierda", placement.characterLocations().get("NARRADOR"));
        assertEquals(2, result.layer().actions().size());
    }

    @Test
    void mapsManifestImagesAndInterventionsToTheatricalCuesOnly() {
        var plan = TheatreGrammarMarkdownParser.parse("""
                # Obra

                - Personaje: NARRADOR | voz=VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO

                ## Acto: El vuelo

                ### Escena: El hangar
                > texto_inicio=5 | texto_fin=10 | mapa_espacial=mapas/mapa-espacial.png

                NARRADOR: Primera replica.
                > origen=extra diegetico | destino=extra diegetico | imagen=fragmentos/uno.png | tono=CALM
                """);
        NarrationScriptDocument script = NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                segment(1, NarrationSegmentType.HEADING, "Ahora veremos: El vuelo del Tornillo Dorado."),
                segment(2, NarrationSegmentType.PARAGRAPH, "Subtitulo del demo."),
                segment(3, NarrationSegmentType.PARAGRAPH, "Personajes."),
                segment(4, NarrationSegmentType.PARAGRAPH, "Escena\u00A01: El hangar."),
                segment(5, NarrationSegmentType.PARAGRAPH, "NARRADOR: Primera replica.")
        ));

        var result = new TheatreExampleSetupService().execute(
                plan,
                script,
                VoiceLibrary.defaults(),
                Map.of(
                        "mapas/mapa-espacial.png", "IMG-MAP",
                        "fragmentos/uno.png", "IMG-UNO"));

        assertEquals("B0005", result.layer().intervenciones().get(0).blockId());
        assertEquals(1, result.imageAssignments().size());
        assertEquals(NarrativeLayerKind.IMAGE, result.imageAssignments().get(0).kind());
        assertEquals("SEG-005", result.imageAssignments().get(0).textRange().segmentId());
        assertEquals("IMG-UNO", result.imageAssignments().get(0).targetId());
    }

    @Test
    void manifestoTextRangesDefineAviadoresGlobalInterventionNumbers() {
        var plan = TheatreGrammarMarkdownParser.parse("""
                # Obra

                ## Acto: El vuelo

                ### Escena: El hangar
                > texto_inicio=5 | texto_fin=10 | mapa_espacial=mapas/mapa-espacial.png

                NARRADOR: Uno.
                CAPITAN BIGOTE: Dos.
                TENIENTE TORNILLO: Tres.
                CAPITAN BIGOTE: Cuatro.
                TENIENTE TORNILLO: Cinco.
                CAPITAN BIGOTE: Seis.

                ### Escena: En el aire
                > texto_inicio=12 | texto_fin=20 | mapa_espacial=mapas/mapa-espacial.png

                TENIENTE TORNILLO: Siete.
                CAPITAN BIGOTE: Ocho.
                TENIENTE TORNILLO: Nueve.
                CAPITAN BIGOTE: Diez.
                NARRADOR: Once.
                TENIENTE TORNILLO: Doce.
                CAPITAN BIGOTE: Trece.
                TENIENTE TORNILLO: Catorce.
                CAPITAN BIGOTE: Quince.

                ### Escena: El aterrizaje
                > texto_inicio=22 | texto_fin=27 | mapa_espacial=mapas/mapa-espacial.png

                TENIENTE TORNILLO: Dieciseis.
                CAPITAN BIGOTE: Diecisiete.
                NARRADOR: Dieciocho.
                TENIENTE TORNILLO: Diecinueve.
                CAPITAN BIGOTE: Veinte.
                NARRADOR: Veintiuno.
                """);

        var result = new TheatreExampleSetupService().execute(
                plan,
                aviadoresScript(),
                VoiceLibrary.defaults(),
                Map.of("mapas/mapa-espacial.png", "IMG-MAP"));

        assertEquals(21, result.layer().intervenciones().size());
        assertEquals("B0005", result.layer().intervenciones().get(0).blockId());
        assertEquals("INTERVENCION-1", result.layer().intervenciones().get(0).id());
        assertEquals("B0010", result.layer().intervenciones().get(5).blockId());
        assertEquals("INTERVENCION-6", result.layer().intervenciones().get(5).id());
        assertEquals("B0012", result.layer().intervenciones().get(6).blockId());
        assertEquals("INTERVENCION-7", result.layer().intervenciones().get(6).id());
        assertEquals("B0020", result.layer().intervenciones().get(14).blockId());
        assertEquals("INTERVENCION-15", result.layer().intervenciones().get(14).id());
        assertEquals("B0022", result.layer().intervenciones().get(15).blockId());
        assertEquals("INTERVENCION-16", result.layer().intervenciones().get(15).id());
        assertEquals("B0027", result.layer().intervenciones().get(20).blockId());
        assertEquals("INTERVENCION-21", result.layer().intervenciones().get(20).id());
        assertEquals("INTERVENCION-1", result.sceneBoundariesStart().get("SCN-EL-HANGAR"));
        assertEquals("INTERVENCION-6", result.sceneBoundariesEnd().get("SCN-EL-HANGAR"));
        assertEquals("INTERVENCION-7", result.sceneBoundariesStart().get("SCN-EN-EL-AIRE"));
        assertEquals("INTERVENCION-15", result.sceneBoundariesEnd().get("SCN-EN-EL-AIRE"));
        assertEquals("INTERVENCION-16", result.sceneBoundariesStart().get("SCN-EL-ATERRIZAJE"));
        assertEquals("INTERVENCION-21", result.sceneBoundariesEnd().get("SCN-EL-ATERRIZAJE"));
    }

    private static NarrationScriptDocument aviadoresScript() {
        return NarrationScriptDocument.create("Demo", "es", "source.docx", List.of(
                segment(1, NarrationSegmentType.HEADING, "Ahora veremos: El vuelo del Tornillo Dorado."),
                segment(2, NarrationSegmentType.PARAGRAPH, "Guion teatral comico corto para dos aviadores."),
                segment(3, NarrationSegmentType.PARAGRAPH, "Personajes."),
                segment(4, NarrationSegmentType.PARAGRAPH, "Escena 1: El hangar."),
                segment(5, NarrationSegmentType.PARAGRAPH, "NARRADOR: Uno."),
                segment(6, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Dos."),
                segment(7, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Tres."),
                segment(8, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Cuatro."),
                segment(9, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Cinco."),
                segment(10, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Seis."),
                segment(11, NarrationSegmentType.PARAGRAPH, "Escena 2: En el aire."),
                segment(12, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Siete."),
                segment(13, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Ocho."),
                segment(14, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Nueve."),
                segment(15, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Diez."),
                segment(16, NarrationSegmentType.PARAGRAPH, "NARRADOR: Once."),
                segment(17, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Doce."),
                segment(18, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Trece."),
                segment(19, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Catorce."),
                segment(20, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Quince."),
                segment(21, NarrationSegmentType.PARAGRAPH, "Escena 3: El aterrizaje."),
                segment(22, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Dieciseis."),
                segment(23, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Diecisiete."),
                segment(24, NarrationSegmentType.PARAGRAPH, "NARRADOR: Dieciocho."),
                segment(25, NarrationSegmentType.PARAGRAPH, "TENIENTE TORNILLO: Diecinueve."),
                segment(26, NarrationSegmentType.PARAGRAPH, "CAPITAN BIGOTE: Veinte."),
                segment(27, NarrationSegmentType.PARAGRAPH, "NARRADOR: Veintiuno."),
                segment(28, NarrationSegmentType.HEADING, "Notas de demo")));
    }

    private static NarrationSegment segment(int textIndex, NarrationSegmentType type, String text) {
        String number = String.format(java.util.Locale.ROOT, "%03d", textIndex);
        String block = "B" + String.format(java.util.Locale.ROOT, "%04d", textIndex);
        return NarrationSegment.of("SEG-" + number, type, "", text, List.of(block));
    }
}
