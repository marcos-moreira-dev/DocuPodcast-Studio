package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Owns scene-specific character image associations for the shell. */
public final class TheatreCharacterImageCoordinator {
    private static final String SCENE_IMAGE_VIEW = "escena";

    public List<TheatreProjectLayer.CharacterImage> images(
            Optional<ProjectSession> maybeSession,
            String characterId,
            String sceneId) {
        String normalizedCharacterId = normalize(characterId);
        String normalizedSceneId = normalize(sceneId);
        if (maybeSession.isEmpty() || normalizedCharacterId.isBlank()) {
            return List.of();
        }
        List<TheatreProjectLayer.CharacterImage> all = maybeSession.get().project().theatre().characterImages().stream()
                .filter(image -> image.characterId().equals(normalizedCharacterId))
                .toList();
        if (normalizedSceneId.isBlank()) {
            return all.stream().filter(image -> image.sceneId().isBlank()).toList();
        }
        ArrayList<TheatreProjectLayer.CharacterImage> result = new ArrayList<>();
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
            String characterId,
            String sceneId,
            String assetId,
            String notes) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de agregar fotos de personaje.", "", "");
        }
        String normalizedCharacterId = normalize(characterId);
        String normalizedSceneId = normalize(sceneId);
        String normalizedAssetId = normalize(assetId);
        if (normalizedCharacterId.isBlank() || normalizedSceneId.isBlank() || normalizedAssetId.isBlank()) {
            return new SaveResult(false, "Selecciona personaje, escena e imagen antes de asociar la foto.", "", "");
        }

        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        String imageId = uniqueImageId(theatre.characterImages(), normalizedCharacterId, normalizedSceneId);
        ArrayList<TheatreProjectLayer.CharacterImage> updated = new ArrayList<>(theatre.characterImages());
        updated.add(new TheatreProjectLayer.CharacterImage(
                imageId,
                normalizedCharacterId,
                normalizedSceneId,
                SCENE_IMAGE_VIEW,
                normalizedAssetId,
                notes));
        session.replaceProject(session.project().withTheatre(replaceCharacterImages(theatre, updated)), true);
        return new SaveResult(true, "Foto agregada a la escena del personaje.", imageId, "");
    }

    public SaveResult replaceAsset(Optional<ProjectSession> maybeSession, String imageId, String assetId) {
        String normalizedImageId = normalize(imageId);
        String normalizedAssetId = normalize(assetId);
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de reemplazar fotos.", "", "");
        }
        if (normalizedImageId.isBlank() || normalizedAssetId.isBlank()) {
            return new SaveResult(false, "Selecciona una foto y una imagen nueva para reemplazarla.", "", "");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.CharacterImage> updated = new ArrayList<>();
        String previousAssetId = "";
        boolean replaced = false;
        for (TheatreProjectLayer.CharacterImage image : theatre.characterImages()) {
            if (image.id().equals(normalizedImageId)) {
                updated.add(new TheatreProjectLayer.CharacterImage(
                        image.id(), image.characterId(), image.sceneId(), image.view(), normalizedAssetId, image.notes()));
                previousAssetId = image.assetId();
                replaced = true;
            } else {
                updated.add(image);
            }
        }
        if (!replaced) {
            return new SaveResult(false, "La foto seleccionada ya no existe.", "", "");
        }
        session.replaceProject(session.project().withTheatre(replaceCharacterImages(theatre, updated)), true);
        return new SaveResult(true, "Foto reemplazada.", normalizedImageId, previousAssetId);
    }

    public SaveResult updateNote(Optional<ProjectSession> maybeSession, String imageId, String notes) {
        String normalizedImageId = normalize(imageId);
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de editar notas.", "", "");
        }
        if (normalizedImageId.isBlank()) {
            return new SaveResult(false, "Selecciona una foto para editar la nota.", "", "");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.CharacterImage> updated = new ArrayList<>();
        boolean replaced = false;
        for (TheatreProjectLayer.CharacterImage image : theatre.characterImages()) {
            if (image.id().equals(normalizedImageId)) {
                updated.add(new TheatreProjectLayer.CharacterImage(
                        image.id(), image.characterId(), image.sceneId(), image.view(), image.assetId(), notes));
                replaced = true;
            } else {
                updated.add(image);
            }
        }
        if (!replaced) {
            return new SaveResult(false, "La foto seleccionada ya no existe.", "", "");
        }
        session.replaceProject(session.project().withTheatre(replaceCharacterImages(theatre, updated)), true);
        return new SaveResult(true, "Nota de foto actualizada.", normalizedImageId, "");
    }

    public SaveResult delete(Optional<ProjectSession> maybeSession, String imageId) {
        String normalizedImageId = normalize(imageId);
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de eliminar fotos.", "", "");
        }
        if (normalizedImageId.isBlank()) {
            return new SaveResult(false, "Selecciona una foto para eliminarla.", "", "");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.CharacterImage> updated = new ArrayList<>();
        String removedAssetId = "";
        for (TheatreProjectLayer.CharacterImage image : theatre.characterImages()) {
            if (image.id().equals(normalizedImageId)) {
                removedAssetId = image.assetId();
            } else {
                updated.add(image);
            }
        }
        if (removedAssetId.isBlank()) {
            return new SaveResult(false, "La foto seleccionada ya no existe.", "", "");
        }
        session.replaceProject(session.project().withTheatre(replaceCharacterImages(theatre, updated)), true);
        return new SaveResult(true, "Foto eliminada de la escena.", normalizedImageId, removedAssetId);
    }

    private static TheatreProjectLayer replaceCharacterImages(
            TheatreProjectLayer theatre,
            List<TheatreProjectLayer.CharacterImage> images) {
        return new TheatreProjectLayer(
                theatre.intervenciones(),
                theatre.characters(),
                theatre.voiceRoleAliases(),
                images,
                theatre.intervencionesVisuales(),
                theatre.intermediateFrames(),
                theatre.acts(),
                theatre.scenes(),
                theatre.positions(),
                theatre.actions(),
                theatre.textActionPlacements(),
                theatre.objectImages(),
                theatre.objects(),
                theatre.audioTracks(),
                theatre.cameraReferences(),
                theatre.cameraCues(),
                theatre.stageBackdrops(),
                theatre.stageBackdropAssignments());
    }

    private static String uniqueImageId(
            List<TheatreProjectLayer.CharacterImage> images,
            String characterId,
            String sceneId) {
        String base = "CHARIMG-" + safeToken(characterId) + "-" + safeToken(sceneId);
        String candidate = base;
        int suffix = 2;
        while (containsImageId(images, candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static boolean containsImageId(List<TheatreProjectLayer.CharacterImage> images, String candidate) {
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
            String characterId,
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
                    .resolve("personajes")
                    .resolve(safeToken(characterId))
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
