package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreSpatialPromptContextBuilderTest {
    @Test
    void buildsPromptContextFromSpeakerPlacementInteractionAndOtherCharacters() {
        TheatreProjectLayer.TextActionPlacement placement = new TheatreProjectLayer.TextActionPlacement(
                "INTERVENCION-2",
                "ESCENA-1",
                "CAPITAN BIGOTE",
                "fondo centro",
                "frente derecha",
                "TENIENTE TORNILLO",
                Map.of(
                        "CAPITAN BIGOTE", "fondo centro",
                        "TENIENTE TORNILLO", "frente derecha",
                        "NARRADOR", "fuera de escena"));

        String context = TheatreSpatialPromptContextBuilder.build(placement, "NARRADOR");

        assertTrue(context.contains("CAPITAN BIGOTE esta ubicado en fondo centro del escenario"));
        assertTrue(context.contains("CAPITAN BIGOTE interactua con TENIENTE TORNILLO, ubicado en frente derecha del escenario"));
        assertTrue(context.contains("Direccion escenica: desde fondo centro del escenario"));
        assertTrue(context.contains("NARRADOR esta ubicado en fuera de escena."));
        assertTrue(context.contains("izquierda y derecha se refieren a la perspectiva del personaje que mira hacia el publico"));
        assertTrue(context.contains("no implica mostrar al publico; no lo renderices salvo que el guion lo solicite"));
    }

    @Test
    void presentsReadableCharacterNameWhileUsingTechnicalIdForSpatialLookup() {
        TheatreProjectLayer.TextActionPlacement placement = new TheatreProjectLayer.TextActionPlacement(
                "INTERVENCION-1",
                "ESCENA-1",
                "CHR-NARRADOR",
                "extra diegetico",
                "extra diegetico",
                "",
                Map.of("CHR-NARRADOR", "extra diegetico"));

        String context = TheatreSpatialPromptContextBuilder.build(placement, "NARRADOR");

        assertTrue(context.contains("NARRADOR esta ubicado en extra diegetico"));
        assertFalse(context.contains("CHR-NARRADOR"));
    }
}
