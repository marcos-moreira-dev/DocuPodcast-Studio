package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreContextExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds theatre intervention units in MD/project order for context export and local image generation. */
public final class TheatreGenerationUnitPlanner {
    public List<TheatreImageGenerationUnit> units(ProjectSession session, NarrationScriptDocument script, TheatreContextExportScope scope) {
        if (session == null || script == null || script.empty()) return List.of();
        TheatreProjectLayer theatre = session.project().theatre();
        Map<String, TheatreProjectLayer.TextActionPlacement> placements = new LinkedHashMap<>();
        theatre.textActionPlacements().forEach(placement -> placements.put(placement.intervencionId(), placement));
        Map<String, TheatreProjectLayer.Scene> scenes = new LinkedHashMap<>();
        theatre.scenes().forEach(scene -> scenes.put(scene.id(), scene));
        Map<String, TheatreProjectLayer.TheatreAct> acts = new LinkedHashMap<>();
        theatre.acts().forEach(act -> acts.put(act.id(), act));
        ArrayList<TheatreImageGenerationUnit> units = new ArrayList<>();
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            TheatreProjectLayer.TextActionPlacement placement = placements.get(intervention.id());
            if (placement == null || !matchesScope(theatre, placement, scope)) continue;
            TheatreProjectLayer.Scene scene = scenes.get(placement.sceneId());
            TheatreProjectLayer.TheatreAct act = scene == null ? null : acts.get(scene.actId());
            NarrationSegment segment = segmentFor(script, intervention);
            String speaker = speaker(segment == null ? "" : segment.narrationText());
            units.add(new TheatreImageGenerationUnit(
                    act == null ? "" : act.id(), act == null ? "Acto sin nombre" : act.displayName(),
                    placement.sceneId(), scene == null ? placement.sceneId() : scene.displayName(),
                    intervention.id(), segment == null ? intervention.blockId() : segment.id(),
                    speaker, segment == null ? "" : segment.narrationText(),
                    TheatreSpatialPromptContextBuilder.build(placement, speaker), null));
        }
        return List.copyOf(units);
    }

    private static boolean matchesScope(TheatreProjectLayer theatre, TheatreProjectLayer.TextActionPlacement placement, TheatreContextExportScope scope) {
        if (scope == null || scope.isAll()) return true;
        return switch (scope.kind()) {
            case INTERVENTION -> placement.intervencionId().equals(scope.id());
            case SCENE -> placement.sceneId().equals(scope.id());
            case ACT -> theatre.scenes().stream().anyMatch(scene -> scene.id().equals(placement.sceneId()) && scene.actId().equals(scope.id()));
            case ALL -> true;
        };
    }

    private static NarrationSegment segmentFor(NarrationScriptDocument script, TheatreProjectLayer.Intervencion intervention) {
        return script.segments().stream()
                .filter(segment -> segment.id().equals(intervention.blockId()) || segment.sourceBlockIds().contains(intervention.blockId()))
                .findFirst()
                .orElse(null);
    }

    private static String speaker(String text) {
        String normalized = text == null ? "" : text.strip();
        int colon = normalized.indexOf(':');
        return colon > 0 && colon < 48 ? normalized.substring(0, colon).strip() : "NARRADOR";
    }
}
