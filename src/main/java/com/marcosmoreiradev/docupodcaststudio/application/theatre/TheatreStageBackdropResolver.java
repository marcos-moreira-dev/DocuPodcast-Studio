package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Resolves stage backdrop assignments, inheriting intervention cues forward over scene defaults. */
public final class TheatreStageBackdropResolver {
    public Optional<ResolvedBackdrop> resolve(DocuPodcastProject project,
                                              String sceneId,
                                              String interventionId,
                                              Path projectDirectory) {
        if (project == null || project.theatre() == null || projectDirectory == null) {
            return Optional.empty();
        }
        String backdropId = effectiveBackdropId(project.theatre(), sceneId, interventionId);
        if (backdropId.isBlank()) {
            return Optional.empty();
        }
        TheatreProjectLayer.StageBackdrop backdrop = backdropsById(project.theatre()).get(backdropId);
        if (backdrop == null) {
            return Optional.empty();
        }
        ProjectAssetCatalog assets = project.assets() == null ? ProjectAssetCatalog.empty() : project.assets();
        Path root = projectDirectory.toAbsolutePath().normalize();
        return assets.byId(backdrop.assetId())
                .filter(ProjectAssetReference::isImage)
                .flatMap(asset -> {
                    Path path = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
                    if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                        return Optional.empty();
                    }
                    return Optional.of(new ResolvedBackdrop(backdrop, asset, path));
                });
    }

    public String effectiveBackdropId(TheatreProjectLayer theatre, String sceneId, String interventionId) {
        if (theatre == null) {
            return "";
        }
        Optional<String> inherited = inheritedInterventionAssignment(theatre, interventionId);
        if (inherited.isPresent()) {
            return TheatreProjectLayer.STAGE_BACKDROP_NONE.equals(inherited.get()) ? "" : inherited.get();
        }
        return assignment(theatre, TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE, sceneId).orElse("");
    }

    private static Optional<String> inheritedInterventionAssignment(TheatreProjectLayer theatre, String interventionId) {
        if (interventionId == null || interventionId.isBlank()) {
            return Optional.empty();
        }
        int target = sequenceOf(theatre, interventionId).orElse(Integer.MAX_VALUE);
        return theatre.stageBackdropAssignments().stream()
                .filter(assignment -> TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION.equals(assignment.scope()))
                .filter(assignment -> sequenceOf(theatre, assignment.scopeId())
                        .map(sequence -> sequence <= target)
                        .orElse(assignment.scopeId().equals(interventionId)))
                .max(Comparator.comparingInt(assignment -> sequenceOf(theatre, assignment.scopeId()).orElse(-1)))
                .map(TheatreProjectLayer.StageBackdropAssignment::backdropId);
    }

    private static Optional<String> assignment(TheatreProjectLayer theatre, String scope, String scopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return Optional.empty();
        }
        return theatre.stageBackdropAssignments().stream()
                .filter(assignment -> scope.equals(assignment.scope()))
                .filter(assignment -> scopeId.equals(assignment.scopeId()))
                .map(TheatreProjectLayer.StageBackdropAssignment::backdropId)
                .findFirst();
    }

    private static Optional<Integer> sequenceOf(TheatreProjectLayer theatre, String interventionId) {
        if (theatre == null || interventionId == null || interventionId.isBlank()) {
            return Optional.empty();
        }
        return theatre.intervenciones().stream()
                .filter(intervention -> intervention.id().equals(interventionId))
                .map(TheatreProjectLayer.Intervencion::sequenceIndex)
                .findFirst();
    }

    private static Map<String, TheatreProjectLayer.StageBackdrop> backdropsById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.StageBackdrop> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.StageBackdrop backdrop : theatre.stageBackdrops()) {
            result.put(backdrop.id(), backdrop);
        }
        return result;
    }

    public record ResolvedBackdrop(
            TheatreProjectLayer.StageBackdrop backdrop,
            ProjectAssetReference asset,
            Path absolutePath
    ) {
        public ResolvedBackdrop {
            absolutePath = absolutePath.toAbsolutePath().normalize();
        }
    }
}
