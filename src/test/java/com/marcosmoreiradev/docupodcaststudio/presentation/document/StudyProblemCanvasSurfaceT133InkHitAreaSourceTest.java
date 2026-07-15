package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyProblemCanvasSurfaceT133InkHitAreaSourceTest {

    @Test
    void inkInputLayerHasConcreteHitAreaForMouseAndStylusFallback() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java"));

        assertTrue(source.contains("private final Rectangle inkInputHitArea = new Rectangle();"));
        assertTrue(source.contains("inkInputHitArea.setFill(Color.TRANSPARENT);"));
        assertTrue(source.contains("inkInputHitArea.setMouseTransparent(false);"));
        assertTrue(source.contains("inkInputLayer.getChildren().add(inkInputHitArea);"));
        assertTrue(source.contains("Rectangle inkInputTarget()"));
        assertTrue(source.contains("return inkInputHitArea;"));
        assertTrue(source.contains("resizeInkInputHitArea(width, height);"));
        assertTrue(source.contains("inkInputHitArea.toBack();"));
    }
}
