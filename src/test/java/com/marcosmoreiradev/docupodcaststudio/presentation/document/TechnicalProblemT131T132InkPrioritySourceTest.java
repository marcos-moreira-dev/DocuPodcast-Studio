package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemT131T132InkPrioritySourceTest {
    @Test
    void drawingModeKeepsInkCaptureAboveImages() throws Exception {
        String source = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");

        assertTrue(source.contains("canvasMode.addListener((obs, oldValue, newValue) -> updateImageInteractionMode())"));
        assertTrue(source.contains("private boolean canInteractWithCanvasImages()"));
        assertTrue(source.contains("boolean imageCropPicking = imageCropMode && selectedCanvasImage != null;"));
        assertTrue(source.contains("boolean imagePicking = imageCropPicking || canInteractWithCanvasImages();"));
        assertTrue(source.contains("drawingSurface.imageLayer().setMouseTransparent(!imagePicking)"));
        assertTrue(source.contains("private boolean canCaptureInkInput()"));
        assertTrue(source.contains("&& !imageInteractionMode.get()"));
        assertTrue(source.contains("&& !imageCropMode"));
        assertTrue(source.contains("(canvasRegionSelectionMode.get() || drawMode.get())"));
        assertTrue(source.contains("selectCanvasImageUnderPointerForDrawing(point)"));
        assertTrue(source.contains("private CanvasImageItem canvasImageAt(Point2D point)"));
        assertTrue(source.contains("boolean inkActive = canCaptureInkInput();"));
        assertTrue(source.contains("boolean imageToolsActive = imageCropMode || canInteractWithCanvasImages();"));
        assertTrue(source.contains("drawingSurface.inkInputLayer().setMouseTransparent(!layerInteractive)"));
        assertTrue(source.contains("drawingSurface.inkInputLayer().setPickOnBounds(inkActive)"));
        assertTrue(source.contains("drawingSurface.inkInputTarget().setMouseTransparent(!inkActive)"));
        assertTrue(source.contains("drawingSurface.inkInputTarget().setDisable(!inkActive)"));
        assertTrue(source.contains("inkInputProvider.attach(drawingSurface.inkInputTarget(),"));
        assertTrue(source.contains("imageInteractionMode.set(false);"));
        assertFalse(source.contains("imageInteractionMode.set(true);"));
        assertFalse(source.contains("inkCaptureHasPriority"));
    }

    @Test
    void pressureOnlyChangesBrushWhenNativeInputReallySupportsIt() throws Exception {
        String source = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");

        assertTrue(source.contains(".pressureWidth(penStrokeWidth(), pressure)"));
        assertTrue(source.contains("penStrokeWidth(),"));
        assertTrue(source.contains("return capabilities.nativeProvider() && capabilities.pressure();"));
        assertTrue(source.contains("point.pressure()"));
        assertTrue(source.contains("jsonDoubleValue(pointObject, \"pressure\", 1)"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
