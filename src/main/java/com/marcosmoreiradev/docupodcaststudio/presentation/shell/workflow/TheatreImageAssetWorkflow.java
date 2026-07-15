package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Imports, replaces and removes theatre character/object image assets. */
public final class TheatreImageAssetWorkflow {
    private final ApplicationServices applicationServices;
    private final TheatreCharacterImageCoordinator characterImages;
    private final TheatreObjectImageCoordinator objectImages;

    public TheatreImageAssetWorkflow(
            ApplicationServices applicationServices,
            TheatreCharacterImageCoordinator characterImages,
            TheatreObjectImageCoordinator objectImages) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
        this.characterImages = Objects.requireNonNull(characterImages, "characterImages");
        this.objectImages = Objects.requireNonNull(objectImages, "objectImages");
    }

    public Result addCharacterImage(ProjectSession session, String characterId, String sceneId, Path imageFile) throws IOException {
        var imported = importImage(session, imageFile, "Guarda el proyecto antes de importar fotos de personaje para mantener rutas relativas.");
        TheatreCharacterImageCoordinator.SaveResult result =
                characterImages.add(Optional.of(session), characterId, sceneId, imported.imageAsset().id(), "");
        characterImages.mirrorSidecar(session, characterId, sceneId, imported.imageAsset(), "");
        return new Result(result.message());
    }

    public Result replaceCharacterImage(ProjectSession session, StoryboardDocument storyboard, String imageId, Path imageFile) throws IOException {
        TheatreProjectLayer.CharacterImage current = characterImageById(session, imageId).orElse(null);
        var imported = importImage(session, imageFile, "Guarda el proyecto antes de reemplazar fotos de personaje.");
        TheatreCharacterImageCoordinator.SaveResult result =
                characterImages.replaceAsset(Optional.of(session), imageId, imported.imageAsset().id());
        if (current != null) {
            characterImages.mirrorSidecar(session, current.characterId(), current.sceneId(), imported.imageAsset(), current.notes());
        }
        removeUnusedProjectImageAsset(session, storyboard, result.previousAssetId());
        return new Result(result.message());
    }

    public Result updateCharacterImageNote(ProjectSession session, String imageId, String note) {
        TheatreCharacterImageCoordinator.SaveResult result = characterImages.updateNote(Optional.of(session), imageId, note);
        characterImageById(session, imageId).flatMap(image -> projectAssetById(session, image.assetId())
                        .map(asset -> new CharacterMirror(image, asset)))
                .ifPresent(mirror -> characterImages.mirrorSidecar(
                        session, mirror.image().characterId(), mirror.image().sceneId(), mirror.asset(), mirror.image().notes()));
        return new Result(result.message());
    }

    public Result deleteCharacterImage(ProjectSession session, StoryboardDocument storyboard, String imageId) {
        TheatreCharacterImageCoordinator.SaveResult result = characterImages.delete(Optional.of(session), imageId);
        removeUnusedProjectImageAsset(session, storyboard, result.previousAssetId());
        return new Result(result.message());
    }

    public Result addObjectImage(ProjectSession session, String objectId, String sceneId, Path imageFile) throws IOException {
        var imported = importImage(session, imageFile, "Guarda el proyecto antes de importar fotos de objeto para mantener rutas relativas.");
        TheatreObjectImageCoordinator.SaveResult result =
                objectImages.add(Optional.of(session), objectId, sceneId, imported.imageAsset().id(), "");
        objectImages.mirrorSidecar(session, objectId, sceneId, imported.imageAsset(), "");
        return new Result(result.message());
    }

    public Result replaceObjectImage(ProjectSession session, StoryboardDocument storyboard, String imageId, Path imageFile) throws IOException {
        TheatreProjectLayer.ObjectImage current = objectImageById(session, imageId).orElse(null);
        var imported = importImage(session, imageFile, "Guarda el proyecto antes de reemplazar fotos de objeto.");
        TheatreObjectImageCoordinator.SaveResult result =
                objectImages.replaceAsset(Optional.of(session), imageId, imported.imageAsset().id());
        if (current != null) {
            objectImages.mirrorSidecar(session, current.objectId(), current.sceneId(), imported.imageAsset(), current.notes());
        }
        removeUnusedProjectImageAsset(session, storyboard, result.previousAssetId());
        return new Result(result.message());
    }

    public Result updateObjectImageNote(ProjectSession session, String imageId, String note) {
        TheatreObjectImageCoordinator.SaveResult result = objectImages.updateNote(Optional.of(session), imageId, note);
        objectImageById(session, imageId).flatMap(image -> projectAssetById(session, image.assetId())
                        .map(asset -> new ObjectMirror(image, asset)))
                .ifPresent(mirror -> objectImages.mirrorSidecar(
                        session, mirror.image().objectId(), mirror.image().sceneId(), mirror.asset(), mirror.image().notes()));
        return new Result(result.message());
    }

    public Result deleteObjectImage(ProjectSession session, StoryboardDocument storyboard, String imageId) {
        TheatreObjectImageCoordinator.SaveResult result = objectImages.delete(Optional.of(session), imageId);
        removeUnusedProjectImageAsset(session, storyboard, result.previousAssetId());
        return new Result(result.message());
    }

    private ImportedImage importImage(ProjectSession session, Path imageFile, String saveMessage) throws IOException {
        Objects.requireNonNull(imageFile, "imageFile");
        Path projectFile = session.projectFile().orElseThrow(() -> new IOException(saveMessage));
        var imported = applicationServices.storyboard().importImageAsset().importImage(session.project(), projectFile, imageFile);
        session.replaceProject(imported.project(), true);
        return new ImportedImage(imported.imageAsset());
    }

    private void removeUnusedProjectImageAsset(ProjectSession session, StoryboardDocument storyboard, String assetId) {
        if (assetId == null || assetId.isBlank()
                || NarrativeLayerCoordinator.projectImageAssetInUse(session.project(), storyboard, assetId)) {
            return;
        }
        session.project().assets().byId(assetId)
                .ifPresent(asset -> ProjectImageManagementWorkflow.deleteAssetFileIfPresent(asset, session.projectFile()));
        session.replaceProject(applicationServices.assets().removeProjectAsset().remove(session.project(), assetId), true);
    }

    private static Optional<TheatreProjectLayer.CharacterImage> characterImageById(ProjectSession session, String imageId) {
        String id = imageId == null ? "" : imageId.strip();
        return id.isBlank() ? Optional.empty()
                : session.project().theatre().characterImages().stream().filter(image -> image.id().equals(id)).findFirst();
    }

    private static Optional<TheatreProjectLayer.ObjectImage> objectImageById(ProjectSession session, String imageId) {
        String id = imageId == null ? "" : imageId.strip();
        return id.isBlank() ? Optional.empty()
                : session.project().theatre().objectImages().stream().filter(image -> image.id().equals(id)).findFirst();
    }

    private static Optional<ProjectAssetReference> projectAssetById(ProjectSession session, String assetId) {
        String id = assetId == null ? "" : assetId.strip();
        return id.isBlank() ? Optional.empty() : session.project().assets().byId(id);
    }

    public record Result(String message) {}
    private record ImportedImage(ProjectAssetReference imageAsset) {}
    private record CharacterMirror(TheatreProjectLayer.CharacterImage image, ProjectAssetReference asset) {}
    private record ObjectMirror(TheatreProjectLayer.ObjectImage image, ProjectAssetReference asset) {}
}
