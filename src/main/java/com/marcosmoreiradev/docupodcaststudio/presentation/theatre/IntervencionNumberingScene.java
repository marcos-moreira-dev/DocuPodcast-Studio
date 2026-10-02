package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Scene-aware presentation numbering for intervention IDs while preserving global source anchors. */
public final class IntervencionNumberingScene {
    private IntervencionNumberingScene() {
    }

    public static List<IntervencionCatalogo.IntervencionInfo> intervencionesParaEscena(
            List<IntervencionCatalogo.IntervencionInfo> globalAliases,
            List<TheatreProjectLayer.Scene> scenes,
            IntervencionBoundaryStore boundaryStore,
            TheatreProjectLayer.Scene scene) {
        if (globalAliases == null || globalAliases.isEmpty()) {
            return List.of();
        }
        int base = indiceBaseNumeracion(globalAliases, scenes, boundaryStore).orElse(0);
        int start = 0;
        int end = globalAliases.size() - 1;
        if (scene != null && boundaryStore != null) {
            IntervencionBoundaryStore.SceneBoundary boundary = boundaryStore.limite(scene.id());
            start = indiceDeIntervencion(globalAliases, boundary.startId()).orElse(start);
            end = indiceDeIntervencion(globalAliases, boundary.endId()).orElse(end);
        }
        if (start > end) {
            int swap = start;
            start = end;
            end = swap;
        }
        ArrayList<IntervencionCatalogo.IntervencionInfo> sceneAliases = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            sceneAliases.add(idParaEscena(globalAliases.get(i), i, base));
        }
        return List.copyOf(sceneAliases);
    }

    public static String idParaEscena(
            String intervencionId,
            List<IntervencionCatalogo.IntervencionInfo> globalAliases,
            List<TheatreProjectLayer.Scene> scenes,
            IntervencionBoundaryStore boundaryStore) {
        if (intervencionId == null || intervencionId.isBlank()) {
            return "";
        }
        int base = indiceBaseNumeracion(globalAliases, scenes, boundaryStore).orElse(0);
        return indiceDeIntervencion(globalAliases, intervencionId)
                .map(index -> "Intervencion " + Math.max(1, index - base + 1))
                .orElse(intervencionId);
    }

    public static Optional<Integer> indiceDeIntervencion(List<IntervencionCatalogo.IntervencionInfo> aliases, String aliasName) {
        if (aliases == null || aliasName == null || aliasName.isBlank()) {
            return Optional.empty();
        }
        for (int i = 0; i < aliases.size(); i++) {
            if (aliasName.equals(aliases.get(i).alias())) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> indiceBaseNumeracion(
            List<IntervencionCatalogo.IntervencionInfo> aliases,
            List<TheatreProjectLayer.Scene> scenes,
            IntervencionBoundaryStore boundaryStore) {
        if (aliases == null || aliases.isEmpty() || scenes == null || boundaryStore == null) {
            return Optional.empty();
        }
        for (TheatreProjectLayer.Scene scene : scenes) {
            if (scene == null) {
                continue;
            }
            Optional<Integer> start = indiceDeIntervencion(aliases, boundaryStore.limite(scene.id()).startId());
            if (start.isPresent()) {
                return start;
            }
        }
        return Optional.empty();
    }

    private static IntervencionCatalogo.IntervencionInfo idParaEscena(
            IntervencionCatalogo.IntervencionInfo alias,
            int globalIndex,
            int baseIndex) {
        int displayNumber = Math.max(1, globalIndex - baseIndex + 1);
        return new IntervencionCatalogo.IntervencionInfo(
                alias.alias(),
                "Intervencion " + displayNumber,
                alias.blockId(),
                alias.preview(),
                alias.fullText(),
                alias.stageDirection());
    }
}
