package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreStageGeometryTest {
    @Test
    void canonicalUnderscorePositionsUseTheSameCoordinatesAsUiLabels() {
        assertEquals(TheatreStageGeometry.pointFor("fondo derecha"),
                TheatreStageGeometry.pointFor("fondo_derecha"));
        assertEquals(TheatreStageGeometry.pointFor("centro izquierda"),
                TheatreStageGeometry.pointFor("centro_izquierda"));
        assertEquals(TheatreStageGeometry.pointFor("frente centro"),
                TheatreStageGeometry.pointFor("frente_centro"));
        assertEquals(TheatreStageGeometry.pointFor("hacia el público"),
                TheatreStageGeometry.pointFor("hacia_el_publico"));
    }

    @Test
    void canonicalUnderscorePositionsAreRecognizedInsteadOfFallingBackToCenter() {
        assertTrue(TheatreStageGeometry.knownPosition("fondo_derecha"));
        assertTrue(TheatreStageGeometry.knownPosition("centro_izquierda"));
        assertTrue(TheatreStageGeometry.knownPosition("frente_centro"));
        assertTrue(TheatreStageGeometry.knownPosition("hacia_el_publico"));
    }

    @Test
    void hyphenatedExternalValuesAreAcceptedToo() {
        assertEquals(TheatreStageGeometry.pointFor("centro derecha"),
                TheatreStageGeometry.pointFor("centro-derecha"));
        assertTrue(TheatreStageGeometry.knownPosition("extra-diegético"));
    }
}
