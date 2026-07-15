package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Owns scene-specific theatre object image associations for the shell. */
public final class TheatreObjectImageCoordinator {
    private static final String SCENE_IMAGE_VIEW = "escena";

    public List<TheatreProjectLayer.ObjectImage> images(
            Optional<ProjectSession> maybeSession,
            String objectId,
            String sceneId) {
        String normalizedObjectId = normalize(objectId);
        String normalizedSceneId = normalize(sceneId);
        if (maybeSession.isEmpty() || normalizedObjectId.isBlank()) {
            return List.of();
        }
        List<TheatreProjectLayer.ObjectImage> all = maybeSession.get().project().theatre().objectImages().stream()
                .filter(image -> image.objectId().equals(normalizedObjectId))
                .toList();
        if (normalizedSceneId.isBlank()) {
            return all.stream().filter(image -> image.sceneId().isBlank()).toList();
        }
        ArrayList<TheatreProjectLayer.ObjectImage> result = new ArrayList<>();
        all.stream()
                .filter(image -> image.sceneId().equals(normalizedSceneId))
                .forEach(result::add);
        all.stream()
                .filter(image -> image.sceneId().isBlank())
                .filter(image -> result.stream().noneMatch(existing -> existing.assetId().equals(image.assetId())))
                .forEach(result::add);
        return List.copyOf(result);
    }

    public SaveResult add(
            Optional<ProjectSession> maybeSession,
            String objectId,
            String sceneId,
            String assetId,
            String notes) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de agregar fotos de objeto.", "", "");
        }
        String normalizedObjectId = normalize(objectId);
        String normalizedSceneId = normalize(sceneId);
        String normalizedAssetId = normalize(assetId);
        if (normalizedObjectId.isBlank() || normalizedSceneId.isBlank() || normalizedAssetId.isBlank()) {
            return new SaveResult(false, "Selecciona objeto, escena e imagen antes de asociar la foto.", "", "");
        }

        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        String imageId = uniqueImageId(theatre.objectImages(), normalizedObjectId, normalizedSceneId);
        ArrayList<TheatreProjectLayer.ObjectImage> updated = new ArrayList<>(theatre.objectImages());
        updated.add(new TheatreProjectLayer.ObjectImage(
                imageId,
                normalizedObjectId,
                normalizedSceneId,
                SCENE_IMAGE_VIEW,
                normalizedAssetId,
                notes));
        session.replaceProject(session.project().withTheatre(replaceObjectImages(theatre, updated)), true);
        return new SaveResult(true, "Foto agregada a la escena del objeto.", imageId, "");
    }

    public SaveResult replaceAsset(Optional<ProjectSession> maybeSession, String imageId, String assetId) {
        String normalizedImageId = normalize(imageId);
        String normalizedAssetId = normalize(assetId);
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de reemplazar fotos de objeto.", "", "");
        }
        if (normalizedImageId.isBlank() || normalizedAssetId.isBlank()) {
            return new SaveResult(false, "Selecciona una foto y una imagen nueva para reemplazarla.", "", "");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.ObjectImage> updated = new ArrayList<>();
        String previousAssetId = "";
        boolean replaced = false;
        for (TheatreProjectLayer.ObjectImage image : theatre.objectImages()) {
            if (image.id().equals(normalizedImageId)) {
                updated.add(new TheatreProjectLayer.ObjectImage(
                        image.id(), image.objectId(), image.sceneId(), image.view(), normalizedAssetId, image.notes()));
                previousAssetId = image.assetId();
                replaced = true;
            } else {
                updated.add(image);
            }
        }
        if (!replaced) {
            return new SaveResult(false, "La foto seleccionada ya no existe.", "", "");
        }
        session.replaceProject(session.project().withTheatre(replaceObjectImages(theatre, updated)), true);
        return new SaveResult(true, "Foto de objeto reemplazada.", normalizedImageId, previousAssetId);
    }

    public SaveResult updateNote(Optional<ProjectSession> maybeSession, String imageId, String notes) {
        String normalizedImageId = normalize(imageId);
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de editar notas.", "", "");
        }
        if (normalizedImageId.isBlank()) {
            return new SaveResult(false, "Selecciona una foto de objeto para editar la nota.", "", "");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.ObjectImage> updated = new ArrayList<>();
        boolean replaced = false;
        for (TheatreProjectLayer.ObjectImage image : theatre.objectImages()) {
            if (image.id().equals(normalizedImageId)) {
                updated.add(new TheatreProjectLayer.ObjectImage(
                        image.id(), image.objectId(), image.sceneId(), image.view(), image.assetId(), notes));
                replaced = true;
            } else {
                updated.add(image);
            }
        }
        if (!replaced) {
            return new SaveResult(false, "La foto seleccionada ya no existe.", "", "");
        }
        session.replaceProject(session.project().withTheatre(replaceObjectImages(theatre, updated)), true);
        return new SaveResult(true, "Nota de foto de objeto actualizada.", normalizedImageId, "");
    }

    public SaveResult delete(Optional<ProjectSession> maybeSession, String imageId) {
        String normalizedImageId = normalize(imageId);
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de eliminar fotos de objeto.", "", "");
        }
        if (normalizedImageId.isBlank()) {
            return new SaveResult(false, "Selecciona una foto de objeto para eliminarla.", "", "");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.ObjectImage> updated = new ArrayList<>();
        String removedAssetId = "";
        for (TheatreProjectLayer.ObjectImage image : theatre.objectImages()) {
            if (image.id().equals(normalizedImageId)) {
                removedAssetId = image.assetId();
            } else {
                updated.add(image);
            }
        }
        if (removedAssetId.isBlank()) {
            return new SaveResult(false, "La foto seleccionada ya no existe.", "", "");
        }
        session.replaceProject(session.project().withTheatre(replaceObjectImages(theatre, updated)), true);
        return new SaveResult(true, "Foto eliminada de la escena del objeto.", normalizedImageId, removedAssetId);
    }

    private static TheatreProjectLayer replaceObjectImages(
            TheatreProjectLayer theatre,
            List<TheatreProjectLayer.ObjectImage> images) {
        return new TheatreProjectLayer(
                theatre.intervenciones(),
                theatre.characters(),
                theatre.voiceRoleAliases(),
                theatre.characterImages(),
                theatre.intervencionesVisuales(),
                theatre.intermediateFrames(),
                theatre.acts(),
                theatre.scenes(),
                theatre.positions(),
                theatre.actions(),
                theatre.textActionPlacements(),
                images,
                theatre.objects(),
                theatre.audioTracks(),
                theatre.cameraReferences(),
                theatre.cameraCues(),
                theatre.stageBackdrops(),
                theatre.stageBackdropAssignments());
    }

    private static String uniqueImageId(
            List<TheatreProjectLayer.ObjectImage> images,
            String objectId,
            String sceneId) {
        String base = "OBJIMG-" + safeToken(objectId) + "-" + safeToken(sceneId);
        String candidate = base;
        int suffix = 2;
        while (containsImageId(images, candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static boolean containsImageId(List<TheatreProjectLayer.ObjectImage> images, String candidate) {
        return images.stream().anyMatch(image -> image.id().equals(candidate));
    }

    private static String safeToken(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "SIN-ID" : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public void mirrorSidecar(
            ProjectSession session,
            String objectId,
            String sceneId,
            ProjectAssetReference asset,
            String note) {
        if (session == null || asset == null) return;
        Optional<Path> projectFile = session.projectFile();
        if (projectFile.isEmpty()) return;
        try {
            Path projectRoot = projectFile.get().toAbsolutePath().normalize().getParent();
            if (projectRoot == null) return;
            Path source = projectRoot.resolve(asset.relativePath()).normalize();
            if (!source.startsWith(projectRoot) || !Files.isRegularFile(source)) return;
            Path targetDirectory = projectRoot
                    .resolve("objetos")
                    .resolve(safeToken(objectId))
                    .resolve("imagenes")
                    .resolve(safeToken(sceneId));
            Files.createDirectories(targetDirectory);
            Path target = targetDirectory.resolve(source.getFileName()).normalize();
            if (!target.startsWith(targetDirectory.normalize())) return;
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(
                    targetDirectory.resolve(asset.id() + "-nota.txt"),
                    note == null || note.isBlank() ? "Sin nota." : note.strip(),
                    StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) {
        }
    }

    public record SaveResult(boolean saved, String message, String imageId, String previousAssetId) {
    }
}
