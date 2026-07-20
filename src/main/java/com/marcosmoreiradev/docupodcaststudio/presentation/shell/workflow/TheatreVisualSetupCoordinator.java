package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreBuiltInCameraCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreCameraReferenceResolver;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreStageBackdropResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Coordinates theatre camera catalog, inherited camera cues and stage backdrop assignment. */
public final class TheatreVisualSetupCoordinator {
    public List<TheatreProjectLayer.CameraReference> cameraReferences(Optional<ProjectSession> session) {
        LinkedHashMap<String, TheatreProjectLayer.CameraReference> references =
                new LinkedHashMap<>(TheatreBuiltInCameraCatalog.referencesById());
        session.ifPresent(value -> value.project().theatre().cameraReferences()
                .forEach(reference -> references.put(reference.id(), reference)));
        return List.copyOf(references.values());
    }

    public String effectiveCameraId(Optional<ProjectSession> session, String interventionId) {
        return session.map(value -> new TheatreCameraReferenceResolver()
                        .effectiveCameraId(value.project().theatre(), interventionId))
                .orElse(TheatreProjectLayer.DEFAULT_CAMERA_ID);
    }

    public Optional<String> cameraImageUri(Optional<ProjectSession> session, Optional<Path> projectDirectory,
                                           String cameraId) {
        if (cameraId == null || cameraId.isBlank()) {
            return Optional.empty();
        }
        Optional<String> projectAssetUri = Optional.empty();
        if (session.isPresent() && projectDirectory.isPresent()) {
            projectAssetUri = session.get().project().theatre().cameraReferences().stream()
                    .filter(reference -> reference.id().equals(cameraId))
                    .findFirst()
                    .flatMap(reference -> session.get().project().assets().byId(reference.assetId()))
                    .flatMap(asset -> assetUri(projectDirectory, asset));
        }
        return projectAssetUri.or(() -> TheatreBuiltInCameraCatalog.resourceUri(cameraId));
    }

    public SaveResult setCameraCue(ProjectSession session, String interventionId, String cameraId) throws IOException {
        String normalizedCameraId = cameraId == null ? "" : cameraId.strip();
        if (normalizedCameraId.isBlank()) {
            throw new IOException("Selecciona un tipo de plano.");
        }
        Map<String, TheatreProjectLayer.CameraReference> references =
                new LinkedHashMap<>(TheatreBuiltInCameraCatalog.referencesById());
        session.project().theatre().cameraReferences().forEach(reference -> references.put(reference.id(), reference));
        boolean known = references.values().stream()
                .anyMatch(reference -> reference.id().equals(normalizedCameraId));
        if (!known) {
            throw new IOException("El tipo de plano no existe en el catalogo: " + normalizedCameraId);
        }
        if (interventionId == null || interventionId.isBlank()) {
            throw new IOException("Selecciona un fragmento teatral antes de cambiar el tipo de plano.");
        }
        ArrayList<TheatreProjectLayer.CameraCue> cues = new ArrayList<>(session.project().theatre().cameraCues().stream()
                .filter(cue -> !cue.intervencionId().equals(interventionId))
                .toList());
        cues.add(new TheatreProjectLayer.CameraCue(interventionId, normalizedCameraId,
                "Cambio de plano heredado desde " + interventionId));
        session.replaceProject(session.project().withTheatre(session.project().theatre().withCameraCues(cues)), true);
        return new SaveResult("Tipo de plano aplicado desde " + interventionId + ": " + normalizedCameraId + ".");
    }

    public SaveResult assignStageBackdrop(WorkspaceApplicationServices services,
                                          ProjectSession session,
                                          String interventionId,
                                          String sceneId,
                                          Path imageFile,
                                          boolean fragmentOverride) throws IOException {
        Path projectFile = session.projectFile().orElseThrow(() ->
                new IOException("Guarda el proyecto antes de asignar fondo de escenario."));
        if (interventionId == null || interventionId.isBlank()) {
            throw new IOException("Selecciona un fragmento teatral antes de asignar fondo.");
        }
        String scope = fragmentOverride ? TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION
                : TheatreProjectLayer.STAGE_BACKDROP_SCOPE_SCENE;
        String scopeId = fragmentOverride ? interventionId : (sceneId == null ? "" : sceneId.strip());
        if (scopeId.isBlank()) {
            throw new IOException("La intervencion seleccionada no tiene escena asignada.");
        }
        var imported = services.generation().storyboard().importImageAsset().importImage(session.project(), projectFile, imageFile);
        DocuPodcastProject project = imported.project();
        String backdropId = "BACKDROP-" + safeTheatreToken(stripExtension(imageFile.getFileName().toString()));
        ArrayList<TheatreProjectLayer.StageBackdrop> backdrops = new ArrayList<>(project.theatre().stageBackdrops().stream()
                .filter(backdrop -> !backdrop.id().equals(backdropId))
                .toList());
        backdrops.add(new TheatreProjectLayer.StageBackdrop(backdropId,
                displayNameFromToken(stripExtension(imageFile.getFileName().toString())),
                imported.imageAsset().id(), "Fondo de escenario"));
        ArrayList<TheatreProjectLayer.StageBackdropAssignment> assignments =
                new ArrayList<>(project.theatre().stageBackdropAssignments().stream()
                        .filter(assignment -> !(assignment.scope().equals(scope) && assignment.scopeId().equals(scopeId)))
                        .toList());
        assignments.add(new TheatreProjectLayer.StageBackdropAssignment(scope, scopeId, backdropId,
                fragmentOverride ? "Override de fragmento" : "Fondo de escena"));
        TheatreProjectLayer theatre = project.theatre().withStageBackdrops(backdrops)
                .withStageBackdropAssignments(assignments);
        session.replaceProject(project.withTheatre(theatre), true);
        return new SaveResult("Fondo de escenario asignado a "
                + (fragmentOverride ? "fragmento " + interventionId : "escena " + scopeId) + ".");
    }

    public SaveResult clearStageBackdropFromIntervention(ProjectSession session, String interventionId) {
        if (interventionId == null || interventionId.isBlank()) {
            return new SaveResult("Selecciona un fragmento teatral antes de quitar el fondo.");
        }
        ArrayList<TheatreProjectLayer.StageBackdropAssignment> assignments =
                new ArrayList<>(session.project().theatre().stageBackdropAssignments().stream()
                        .filter(assignment -> !(assignment.scope().equals(TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION)
                                && assignment.scopeId().equals(interventionId)))
                        .toList());
        assignments.add(new TheatreProjectLayer.StageBackdropAssignment(
                TheatreProjectLayer.STAGE_BACKDROP_SCOPE_INTERVENTION,
                interventionId,
                TheatreProjectLayer.STAGE_BACKDROP_NONE,
                "Sin fondo heredado desde " + interventionId));
        session.replaceProject(session.project().withTheatre(
                session.project().theatre().withStageBackdropAssignments(assignments)), true);
        return new SaveResult("Fondo quitado desde " + interventionId + ". Se aplicara el valor por defecto.");
    }

    public StageBackdropPreview stageBackdropPreview(Optional<ProjectSession> session,
                                                     Optional<Path> projectDirectory,
                                                     String interventionId,
                                                     String sceneId) {
        if (session.isEmpty()) {
            return StageBackdropPreview.unassigned();
        }
        return new TheatreStageBackdropResolver()
                .resolve(session.get().project(), sceneId, interventionId, projectDirectory.orElse(null))
                .map(backdrop -> new StageBackdropPreview(
                        backdrop.backdrop().displayName(),
                        assetUri(projectDirectory, backdrop.asset()).orElse(""),
                        true))
                .orElseGet(StageBackdropPreview::unassigned);
    }

    private static Optional<String> assetUri(Optional<Path> projectDirectory, ProjectAssetReference asset) {
        if (projectDirectory.isEmpty() || asset == null) return Optional.empty();
        Path root = projectDirectory.get().toAbsolutePath().normalize();
        Path resolved = root.resolve(asset.relativePath()).normalize();
        if (!resolved.startsWith(root)) return Optional.empty();
        return Optional.of(resolved.toUri().toString());
    }

    private static String stripExtension(String filename) {
        String value = filename == null ? "" : filename.strip();
        int dot = value.lastIndexOf('.');
        return dot <= 0 ? value : value.substring(0, dot);
    }

    private static String safeTheatreToken(String value) {
        String normalized = java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "REFERENCIA" : normalized;
    }

    private static String displayNameFromToken(String value) {
        return safeTheatreToken(value).replace('_', ' ');
    }

    public record SaveResult(String message) {
        public SaveResult {
            message = message == null ? "" : message.strip();
        }
    }

    public record StageBackdropPreview(String displayName, String uri, boolean assigned) {
        public StageBackdropPreview {
            displayName = displayName == null || displayName.isBlank()
                    ? "Fondo no asignado (se aplica por defecto)"
                    : displayName.strip();
            uri = uri == null ? "" : uri.strip();
        }

        public static StageBackdropPreview unassigned() {
            return new StageBackdropPreview("Fondo no asignado (se aplica por defecto)", "", false);
        }
    }
}
