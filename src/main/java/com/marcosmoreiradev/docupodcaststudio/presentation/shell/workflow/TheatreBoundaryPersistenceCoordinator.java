package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreExampleSetupService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionBoundaryStore;
import com.marcosmoreiradev.docupodcaststudio.presentation.theatre.IntervencionCatalogo;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Persists theatrical text-map scene boundaries in project view state. */
public final class TheatreBoundaryPersistenceCoordinator {
    private static final String VIEW_PREFIX = "theatre.sceneBoundary.";
    private static final String SEPARATOR = "|";

    public void hydrate(DocuPodcastProject project, IntervencionBoundaryStore store) {
        Map<String, IntervencionBoundaryStore.SceneBoundary> restored = new LinkedHashMap<>();
        if (project != null) {
            project.viewState().forEach((key, value) -> {
                if (key == null || !key.startsWith(VIEW_PREFIX)) {
                    return;
                }
                String sceneId = key.substring(VIEW_PREFIX.length()).strip();
                String[] parts = (value == null ? "" : value).split("\\|", -1);
                String startId = parts.length > 0 ? parts[0].strip() : "";
                String endId = parts.length > 1 ? parts[1].strip() : "";
                if (!sceneId.isBlank()) {
                    restored.put(sceneId, new IntervencionBoundaryStore.SceneBoundary(startId, endId));
                }
            });
        }
        store.replaceAll(restored);
    }

    public void persist(ProjectSession session, IntervencionBoundaryStore store) {
        if (session == null || store == null) {
            return;
        }
        DocuPodcastProject project = session.project();
        for (String key : project.viewState().keySet().stream()
                .filter(value -> value != null && value.startsWith(VIEW_PREFIX))
                .toList()) {
            project = project.withViewState(key, null);
        }
        for (Map.Entry<String, IntervencionBoundaryStore.SceneBoundary> entry : store.snapshot().entrySet()) {
            String sceneId = entry.getKey() == null ? "" : entry.getKey().strip();
            IntervencionBoundaryStore.SceneBoundary boundary = entry.getValue();
            if (!sceneId.isBlank() && boundary != null) {
                project = project.withViewState(
                        VIEW_PREFIX + sceneId,
                        boundary.startId() + SEPARATOR + boundary.endId());
            }
        }
        if (project != session.project()) {
            session.replaceProject(project, true);
        }
    }

    public void applySetupResult(
            TheatreExampleSetupService.TheatreSetupResult result,
            ReadableDocument document,
            NarrationScriptDocument script,
            IntervencionBoundaryStore store) {
        Map<String, String> aliasByBlockId = new LinkedHashMap<>();
        result.layer().intervenciones().forEach(intervention ->
                aliasByBlockId.put(intervention.blockId(), intervention.id()));
        if (aliasByBlockId.isEmpty()) {
            IntervencionCatalogo.intervenciones(document, script)
                    .forEach(info -> aliasByBlockId.put(info.blockId(), info.alias()));
        }
        boolean appliedTextRanges = false;
        for (Map.Entry<String, TheatreExampleSetupService.SceneTextRange> entry : result.sceneTextRanges().entrySet()) {
            String startAlias = aliasByBlockId.get(blockIdForTextIndex(entry.getValue().startTextIndex()));
            String endAlias = aliasByBlockId.get(blockIdForTextIndex(entry.getValue().endTextIndex()));
            if (startAlias == null || startAlias.isBlank() || endAlias == null || endAlias.isBlank()) {
                continue;
            }
            store.setInicio(entry.getKey(), startAlias);
            store.setFin(entry.getKey(), endAlias);
            appliedTextRanges = true;
        }
        if (appliedTextRanges) {
            return;
        }
        result.sceneBoundariesStart().forEach(store::setInicio);
        result.sceneBoundariesEnd().forEach(store::setFin);
    }

    private static String blockIdForTextIndex(int textIndex) {
        return "B" + String.format(Locale.ROOT, "%04d", textIndex);
    }
}
