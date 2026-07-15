package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StoryboardAsDocumentLayerSourceTest {
    @Test
    void storyboardIsModeledAsDocumentLayerWithSpokenDurationAndAssetReuse() throws Exception {
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/storyboard/BuildStoryboardFromImageLayersUseCase.java"));
        String services = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/StoryboardApplicationServices.java"));
        String storyboard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/storyboard/StoryboardDocument.java"));
        String railProjection = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentRailProjectionFactory.java"));
        String imageProjection = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentRailImagePresentation.java"));
        String roadmap = Files.readString(Path.of("docs/productizacion/STORYBOARD_COMO_CAPA_DOCUMENTO_T75.md"));

        assertTrue(useCase.contains("NarrativeLayerKind.IMAGE"));
        assertTrue(useCase.contains("duration"), "El binding debe declarar que la duración del frame viene del texto hablado");
        assertTrue(useCase.contains("spoken-segment"));
        assertTrue(useCase.contains("ProjectAssetKind.IMAGE") && useCase.contains("ProjectAssetKind.THUMBNAIL"));
        assertTrue(services.contains("BuildStoryboardFromImageLayersUseCase"));
        assertTrue(storyboard.contains("bindingsForImage"));
        assertTrue(storyboard.contains("bindingsByImageAssetId"));
        assertTrue(railProjection.contains("bindingsByImageAssetId"));
        assertTrue(imageProjection.contains("assignmentCount"));
        assertTrue(roadmap.contains("una imagen puede reutilizarse en varios fragmentos"));
        assertTrue(roadmap.contains("dura lo que dura el texto hablado"));
    }
}
