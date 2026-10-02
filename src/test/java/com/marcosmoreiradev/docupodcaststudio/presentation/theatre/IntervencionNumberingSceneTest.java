package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntervencionNumberingSceneTest {
    @Test
    void sceneDisplayNumberNeverReplacesCanonicalAlias() {
        List<IntervencionCatalogo.IntervencionInfo> global = List.of(
                info("INTERVENCION-10001", "B-INTERVENCION-10001", true),
                info("INTERVENCION-10002", "B-INTERVENCION-10002", true),
                info("INTERVENCION-1", "B-INTERVENCION-1", false),
                info("INTERVENCION-2", "B-INTERVENCION-2", false),
                info("INTERVENCION-10003", "B-INTERVENCION-10003", true),
                info("INTERVENCION-3", "B-INTERVENCION-3", false),
                info("INTERVENCION-10004", "B-INTERVENCION-10004", true),
                info("INTERVENCION-4", "B-INTERVENCION-4", false),
                info("INTERVENCION-5", "B-INTERVENCION-5", false),
                info("INTERVENCION-10005", "B-INTERVENCION-10005", true),
                info("INTERVENCION-6", "B-INTERVENCION-6", false));
        TheatreProjectLayer.Scene scene = new TheatreProjectLayer.Scene(
                "SCENE-1", "Cuadro I", "", "", "");
        IntervencionBoundaryStore boundaries = new IntervencionBoundaryStore();
        boundaries.replaceAll(Map.of(scene.id(), new IntervencionBoundaryStore.SceneBoundary(
                "INTERVENCION-10001", "INTERVENCION-6")));

        List<IntervencionCatalogo.IntervencionInfo> sceneAliases =
                IntervencionNumberingScene.intervencionesParaEscena(
                        global, List.of(scene), boundaries, scene);

        IntervencionCatalogo.IntervencionInfo campesino = sceneAliases.get(5);
        assertEquals("INTERVENCION-3", campesino.alias());
        assertEquals("Intervencion 6", campesino.displayName());
        assertEquals("B-INTERVENCION-3", campesino.blockId());
        IntervencionCatalogo.IntervencionInfo concha = sceneAliases.get(10);
        assertEquals("INTERVENCION-6", concha.alias());
        assertEquals("Intervencion 11", concha.displayName());
        assertTrue(sceneAliases.getFirst().stageDirection());
    }

    private static IntervencionCatalogo.IntervencionInfo info(
            String alias, String blockId, boolean stageDirection) {
        return new IntervencionCatalogo.IntervencionInfo(
                alias, alias, blockId, alias, alias, stageDirection);
    }
}
