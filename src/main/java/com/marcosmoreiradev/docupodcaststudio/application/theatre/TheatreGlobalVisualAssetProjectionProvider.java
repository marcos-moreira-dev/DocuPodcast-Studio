package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualAssetTracePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualGlobalAssetProjectionProvider;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualSlotState;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetRole;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentAssetSource;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Projects theatre-owned global references without coupling the visual core to theatre. */
public final class TheatreGlobalVisualAssetProjectionProvider implements VisualGlobalAssetProjectionProvider {
    private final VisualAssetTracePolicy tracePolicy;

    public TheatreGlobalVisualAssetProjectionProvider(VisualAssetTracePolicy tracePolicy) {
        this.tracePolicy = tracePolicy == null ? new VisualAssetTracePolicy() : tracePolicy;
    }

    @Override
    public List<VisualSlotState> globalVisuals(DocuPodcastProject project, Path projectDirectory) {
        if (project == null || project.theatre() == null) {
            return List.of();
        }
        ProjectAssetCatalog assets = project.assets();
        TheatreProjectLayer theatre = project.theatre();
        ArrayList<VisualSlotState> result = new ArrayList<>();
        for (TheatreProjectLayer.CharacterImage image : theatre.characterImages()) {
            result.add(globalSlot("THEATRE-CHAR-" + firstPresent(
                            image.id(), image.characterId() + "-" + image.view()),
                    image.assetId(), assets, projectDirectory, "character", Map.of(
                            "characterId", image.characterId(),
                            "sceneId", image.sceneId(),
                            "view", image.view())));
        }
        for (TheatreProjectLayer.ObjectImage image : theatre.objectImages()) {
            result.add(globalSlot("THEATRE-OBJ-" + firstPresent(
                            image.id(), image.objectId() + "-" + image.view()),
                    image.assetId(), assets, projectDirectory, "object", Map.of(
                            "objectId", image.objectId(),
                            "sceneId", image.sceneId(),
                            "view", image.view())));
        }
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            if (!scene.spatialMapAssetId().isBlank()) {
                result.add(globalSlot("THEATRE-MAP-" + scene.id(), scene.spatialMapAssetId(),
                        assets, projectDirectory, "spatialMap",
                        Map.of("sceneId", scene.id(), "sceneName", scene.displayName())));
            }
        }
        return List.copyOf(result);
    }

    private VisualSlotState globalSlot(
            String bindingId,
            String assetId,
            ProjectAssetCatalog assets,
            Path projectDirectory,
            String theatreKind,
            Map<String, String> metadata
    ) {
        String path = assets.byId(assetId).map(ProjectAssetReference::relativePath).orElse("");
        boolean missing = tracePolicy.missingAsset(assets, assetId, path, projectDirectory);
        LinkedHashMap<String, String> meta = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        meta.put("theatreKind", theatreKind);
        return new VisualSlotState(FragmentAssetRole.THEATRE_VISUAL, bindingId, assetId, path,
                FragmentAssetSource.THEATRE, missing ? "MISSING" : "READY",
                "", theatreKind, missing, meta);
    }

    private static String firstPresent(String first, String fallback) {
        String normalized = normalize(first);
        return normalized.isBlank() ? normalize(fallback) : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
