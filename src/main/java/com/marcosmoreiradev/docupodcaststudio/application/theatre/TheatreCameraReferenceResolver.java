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

/** Resolves inherited theatre camera cues. */
public final class TheatreCameraReferenceResolver {
    public String effectiveCameraId(TheatreProjectLayer theatre, String interventionId) {
        if (theatre == null || interventionId == null || interventionId.isBlank()) {
            return defaultCameraId(theatre);
        }
        int targetSequence = sequenceOf(theatre, interventionId).orElse(Integer.MAX_VALUE);
        return theatre.cameraCues().stream()
                .filter(cue -> sequenceOf(theatre, cue.intervencionId())
                        .map(sequence -> sequence <= targetSequence)
                        .orElse(cue.intervencionId().equals(interventionId)))
                .max(Comparator.comparingInt(cue -> sequenceOf(theatre, cue.intervencionId()).orElse(-1)))
                .map(TheatreProjectLayer.CameraCue::cameraId)
                .orElse(defaultCameraId(theatre));
    }

    public Optional<ResolvedCamera> resolve(DocuPodcastProject project, String interventionId, Path projectDirectory) {
        if (project == null || project.theatre() == null) {
            return Optional.empty();
        }
        String cameraId = effectiveCameraId(project.theatre(), interventionId);
        TheatreProjectLayer.CameraReference reference = referencesById(project.theatre()).get(cameraId);
        if (reference == null) {
            reference = referencesById(project.theatre()).get(defaultCameraId(project.theatre()));
        }
        if (reference == null) {
            return Optional.empty();
        }
        ProjectAssetCatalog assets = project.assets() == null ? ProjectAssetCatalog.empty() : project.assets();
        Path root = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
        TheatreProjectLayer.CameraReference finalReference = reference;
        if (root != null) {
            Optional<ResolvedCamera> projectAsset = assets.byId(reference.assetId())
                    .filter(ProjectAssetReference::isImage)
                    .flatMap(asset -> {
                    Path path = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
                    if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                        return Optional.empty();
                    }
                    return Optional.of(new ResolvedCamera(finalReference, asset, path));
                });
            if (projectAsset.isPresent()) {
                return projectAsset;
            }
        }
        return TheatreBuiltInCameraCatalog.resourcePath(finalReference.id())
                .filter(Files::isRegularFile)
                .map(path -> new ResolvedCamera(finalReference, null, path));
    }

    public boolean sameEffectiveCamera(TheatreProjectLayer theatre, String firstInterventionId, String secondInterventionId) {
        return effectiveCameraId(theatre, firstInterventionId)
                .equals(effectiveCameraId(theatre, secondInterventionId));
    }

    private static String defaultCameraId(TheatreProjectLayer theatre) {
        if (theatre != null) {
            Optional<String> defaultFromCatalog = theatre.cameraReferences().stream()
                    .filter(TheatreProjectLayer.CameraReference::defaultCamera)
                    .map(TheatreProjectLayer.CameraReference::id)
                    .findFirst();
            if (defaultFromCatalog.isPresent()) {
                return defaultFromCatalog.get();
            }
        }
        return TheatreProjectLayer.DEFAULT_CAMERA_ID;
    }

    private static Map<String, TheatreProjectLayer.CameraReference> referencesById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.CameraReference> result = new LinkedHashMap<>();
        result.putAll(TheatreBuiltInCameraCatalog.referencesById());
        if (theatre != null) {
            for (TheatreProjectLayer.CameraReference reference : theatre.cameraReferences()) {
                result.put(reference.id(), reference);
            }
        }
        return result;
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

    public record ResolvedCamera(
            TheatreProjectLayer.CameraReference reference,
            ProjectAssetReference asset,
            Path absolutePath
    ) {
        public ResolvedCamera {
            absolutePath = absolutePath.toAbsolutePath().normalize();
        }
    }
}
