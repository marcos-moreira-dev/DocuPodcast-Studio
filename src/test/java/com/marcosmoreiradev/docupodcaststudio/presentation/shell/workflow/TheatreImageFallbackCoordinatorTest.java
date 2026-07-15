package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TheatreImageFallbackCoordinatorTest {
    @TempDir
    Path tempDirectory;

    @Test
    void characterSceneImagesIncludeGlobalImagesAsFallback() {
        ProjectSession session = ProjectSession.opened(projectWithGlobalImages(), tempDirectory.resolve("obra.docupodcast.json"));
        TheatreCharacterImageCoordinator coordinator = new TheatreCharacterImageCoordinator();

        List<String> sceneAssets = coordinator.images(Optional.of(session), "CHR-NARRADOR", "SCN-HANGAR").stream()
                .map(TheatreProjectLayer.CharacterImage::assetId)
                .toList();
        List<String> otherSceneAssets = coordinator.images(Optional.of(session), "CHR-NARRADOR", "SCN-AIRE").stream()
                .map(TheatreProjectLayer.CharacterImage::assetId)
                .toList();
        List<String> globalAssets = coordinator.images(Optional.of(session), "CHR-NARRADOR", "").stream()
                .map(TheatreProjectLayer.CharacterImage::assetId)
                .toList();

        assertEquals(List.of("IMG-NARRADOR-ESCENA", "IMG-NARRADOR-GLOBAL"), sceneAssets);
        assertEquals(List.of("IMG-NARRADOR-GLOBAL"), otherSceneAssets);
        assertEquals(List.of("IMG-NARRADOR-GLOBAL"), globalAssets);
    }

    @Test
    void objectSceneImagesIncludeGlobalImagesAsFallback() {
        ProjectSession session = ProjectSession.opened(projectWithGlobalImages(), tempDirectory.resolve("obra.docupodcast.json"));
        TheatreObjectImageCoordinator coordinator = new TheatreObjectImageCoordinator();

        List<String> sceneAssets = coordinator.images(Optional.of(session), "OBJ-MAPA", "SCN-HANGAR").stream()
                .map(TheatreProjectLayer.ObjectImage::assetId)
                .toList();
        List<String> otherSceneAssets = coordinator.images(Optional.of(session), "OBJ-MAPA", "SCN-AIRE").stream()
                .map(TheatreProjectLayer.ObjectImage::assetId)
                .toList();
        List<String> globalAssets = coordinator.images(Optional.of(session), "OBJ-MAPA", "").stream()
                .map(TheatreProjectLayer.ObjectImage::assetId)
                .toList();

        assertEquals(List.of("IMG-MAPA-ESCENA", "IMG-MAPA-GLOBAL"), sceneAssets);
        assertEquals(List.of("IMG-MAPA-GLOBAL"), otherSceneAssets);
        assertEquals(List.of("IMG-MAPA-GLOBAL"), globalAssets);
    }

    private static DocuPodcastProject projectWithGlobalImages() {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(),
                List.of(new TheatreProjectLayer.CharacterProfile("CHR-NARRADOR", "NARRADOR", List.of(), "")),
                List.of(),
                List.of(
                        new TheatreProjectLayer.CharacterImage("CHARIMG-NARRADOR-GLOBAL", "CHR-NARRADOR", "", "Frontal", "IMG-NARRADOR-GLOBAL", ""),
                        new TheatreProjectLayer.CharacterImage("CHARIMG-NARRADOR-HANGAR", "CHR-NARRADOR", "SCN-HANGAR", "Frontal", "IMG-NARRADOR-ESCENA", "")),
                List.of(),
                List.of(new TheatreProjectLayer.TheatreAct("ACT-001", "Acto 1", "")),
                List.of(new TheatreProjectLayer.Scene("SCN-HANGAR", "El hangar", "", "ACT-001", "IMG-MAPA")),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new TheatreProjectLayer.ObjectImage("OBJIMG-MAPA-GLOBAL", "OBJ-MAPA", "", "Referencia", "IMG-MAPA-GLOBAL", ""),
                        new TheatreProjectLayer.ObjectImage("OBJIMG-MAPA-HANGAR", "OBJ-MAPA", "SCN-HANGAR", "Referencia", "IMG-MAPA-ESCENA", "")),
                List.of(new TheatreProjectLayer.TheatreObject("OBJ-MAPA", "Mapa de ruta", "")));
        return DocuPodcastProject.createNew("Obra").withTheatre(theatre);
    }
}
