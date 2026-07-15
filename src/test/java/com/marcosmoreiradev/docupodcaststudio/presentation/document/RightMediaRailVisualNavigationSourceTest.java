package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RightMediaRailVisualNavigationSourceTest {
    @Test
    void rightRailIsVisualNavigationNotAnAssignmentForm() throws Exception {
        String mediaRail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String mediaCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/MediaThumbnailCard.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"));
        String appStyles = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java"));

        assertTrue(mediaRail.contains("sentence-level visual references"));
        assertTrue(mediaRail.contains("Fragmentos visuales"));
        assertTrue(mediaRail.contains("storyboardItems"));
        assertTrue(mediaRail.contains("selectDocumentFragmentRailItem"));
        assertFalse(mediaRail.contains("looseImageItems"));
        assertTrue(mediaRail.contains("Borrar imágenes"));
        assertFalse(mediaRail.contains("Asignadas"));
        assertTrue(mediaCard.contains("String relation"));
        assertTrue(mediaCard.contains("String description"));
        assertTrue(mediaCard.contains("Tooltip.install"));
        assertTrue(appStyles.contains("UI_MEDIA_THUMBNAIL_CARD"));
        assertTrue(css.contains("ui-media-thumbnail-card"));
        assertFalse(mediaRail.contains("Elegir imagen"));
        assertFalse(mediaRail.contains("Asignar voz"));
        assertFalse(mediaRail.contains("Asignar audio"));
        assertFalse(mediaRail.contains("Extraer audio"));
    }
}
