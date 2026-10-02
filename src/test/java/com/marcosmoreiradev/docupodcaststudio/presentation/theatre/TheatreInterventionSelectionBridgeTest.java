package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreInterventionSelectionBridgeTest {
    @Test
    void globalDocumentSelectionUsesGlobalAliasWhenNoSceneIsFocused() {
        String alias = TheatreInterventionSelectionBridge.aliasForBlock(
                "B0003",
                "",
                aliases(),
                scenes(),
                new IntervencionBoundaryStore());

        assertEquals("INTERVENCION-3", alias);
    }

    @Test
    void focusedSceneSelectionFallsBackAcrossBoundaryForDocumentClicks() {
        IntervencionBoundaryStore store = new IntervencionBoundaryStore();
        store.setInicio("SC-2", "INTERVENCION-2");
        store.setFin("SC-2", "INTERVENCION-3");

        assertEquals("INTERVENCION-2", TheatreInterventionSelectionBridge.aliasForBlock(
                "B0002", "SC-2", aliases(), scenes(), store));
        assertEquals("INTERVENCION-3", TheatreInterventionSelectionBridge.aliasForBlock(
                "B0003", "SC-2", aliases(), scenes(), store));
        assertEquals("INTERVENCION-4", TheatreInterventionSelectionBridge.aliasForBlock(
                "B0004", "SC-2", aliases(), scenes(), store));
    }

    @Test
    void stageDirectionAfterSceneHeaderResolvesToTheNewScene() {
        List<IntervencionCatalogo.IntervencionInfo> aliases = List.of(
                alias(1, "B-INTERVENCION-1"),
                new IntervencionCatalogo.IntervencionInfo(
                        "INTERVENCION-10026", "Intervencion 2", "B-INTERVENCION-10026",
                        "Acotación: Blackout", "Acotación: Blackout", true),
                alias(2, "B-INTERVENCION-2"));
        List<TheatreProjectLayer.Scene> scenes = scenes();
        IntervencionBoundaryStore store = new IntervencionBoundaryStore();
        store.replaceAll(Map.of(
                "SC-1", new IntervencionBoundaryStore.SceneBoundary("INTERVENCION-1", "INTERVENCION-1"),
                "SC-2", new IntervencionBoundaryStore.SceneBoundary("INTERVENCION-10026", "INTERVENCION-2")));

        assertEquals("SC-2", TheatreInterventionSelectionBridge.sceneForBlock(
                "B-INTERVENCION-10026", aliases, scenes, store).orElseThrow().id());
        assertEquals("INTERVENCION-10026", TheatreInterventionSelectionBridge.aliasForBlock(
                "B-INTERVENCION-10026", "SC-1", aliases, scenes, store));
    }

    private static List<IntervencionCatalogo.IntervencionInfo> aliases() {
        return List.of(
                alias(1, "B0001"),
                alias(2, "B0002"),
                alias(3, "B0003"),
                alias(4, "B0004"));
    }

    private static List<TheatreProjectLayer.Scene> scenes() {
        return List.of(
                new TheatreProjectLayer.Scene("SC-1", "Escena 1", "", "ACT-1"),
                new TheatreProjectLayer.Scene("SC-2", "Escena 2", "", "ACT-1"));
    }

    private static IntervencionCatalogo.IntervencionInfo alias(int index, String blockId) {
        return new IntervencionCatalogo.IntervencionInfo(
                "INTERVENCION-" + index,
                "Intervencion " + index,
                blockId,
                "PERSONAJE: texto " + index,
                "PERSONAJE: texto " + index);
    }
}
