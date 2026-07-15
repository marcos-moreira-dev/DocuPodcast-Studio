package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Filters narration script segments to the theatrical act/scene requested for export. */
public final class TheatreExportScopeScriptFilter {
    public NarrationScriptDocument filter(ProjectSession session, NarrationScriptDocument script, TheatreExportScope scope) {
        if (script == null || script.empty() || scope == null || scope.isAll()) {
            return script;
        }
        Set<String> blockIds = theatreBlockIdsForScope(session, scope);
        if (blockIds.isEmpty()) {
            return new NarrationScriptDocument(script.id(), script.title() + " - porcion teatral vacia",
                    script.language(), script.sourceDocumentTitle(), List.of(), script.createdAt(), Instant.now(), script.notes());
        }
        List<NarrationSegment> segments = script.segments().stream()
                .filter(segment -> segment.sourceBlockIds().stream().anyMatch(blockIds::contains)
                        || blockIds.contains(segment.id()))
                .toList();
        return new NarrationScriptDocument(script.id(), script.title() + " - porcion teatral",
                script.language(), script.sourceDocumentTitle(), segments, script.createdAt(), Instant.now(), script.notes());
    }

    private static Set<String> theatreBlockIdsForScope(ProjectSession session, TheatreExportScope scope) {
        if (session == null || scope == null || scope.isAll()) {
            return Set.of();
        }
        TheatreProjectLayer theatre = session.project().theatre();
        Set<String> sceneIds = new LinkedHashSet<>();
        if (scope.kind() == TheatreExportScope.Kind.SCENE) {
            sceneIds.add(scope.id());
        } else if (scope.kind() == TheatreExportScope.Kind.ACT) {
            theatre.scenes().stream()
                    .filter(scene -> scene.actId().equals(scope.id()))
                    .map(TheatreProjectLayer.Scene::id)
                    .forEach(sceneIds::add);
        }
        Map<String, TheatreProjectLayer.TextActionPlacement> placements = new LinkedHashMap<>();
        theatre.textActionPlacements().forEach(placement -> placements.put(placement.intervencionId(), placement));
        LinkedHashSet<String> blockIds = new LinkedHashSet<>();
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            TheatreProjectLayer.TextActionPlacement placement = placements.get(intervention.id());
            if (placement != null && sceneIds.contains(placement.sceneId())) {
                blockIds.add(intervention.blockId());
            }
        }
        return blockIds;
    }
}
