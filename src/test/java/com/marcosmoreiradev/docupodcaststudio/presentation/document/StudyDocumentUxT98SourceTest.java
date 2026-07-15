package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyDocumentUxT98SourceTest {
    @Test
    void canvasDrawsInkAboveTransferredImagesAndExportsHighQualityContent() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String canvas = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");

        assertTrue(canvas.contains("getChildren().addAll(backgroundLayer, imageLayer, strokeLayer, liveStrokeLayer, inkInputLayer)"));
        assertTrue(canvas.contains("strokeLayer.setMouseTransparent(true)"));
        assertTrue(canvas.contains("copyImages(canvasImages, output, size.scale(), size.originX(), size.originY())"));
        assertTrue(canvas.contains("copyInk(output, size)"));
        assertTrue(canvas.contains("EXPORT_SCALE = 4"));
        assertTrue(canvas.contains("MAX_EXPORT_PIXELS"));
        assertTrue(canvas.contains("StudyProblemCanvasExportResult exportWithImages"));
        assertTrue(canvas.contains("MAX_CANVAS_ROWS = 16"));
        assertTrue(dialog.contains("MAX_UNDO_SNAPSHOTS = 50"));
        assertFalse(dialog.contains("private static final class TiledCanvasSurface"));
    }

    @Test
    void transferredImagesHaveResizeHandlesWithoutVerticalToolbarText() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String css = read("src/main/resources/css/document/study-problem.css");

        assertTrue(dialog.contains("imageResizeHandles"));
        assertTrue(dialog.contains("technical-problem-image-resize-handle"));
        assertTrue(dialog.contains("resizeSelectedImageFromHandle"));
        assertTrue(dialog.contains("imageRatio(view, width, height)"));
        assertTrue(dialog.contains("Transferir todas al lienzo"));
        assertTrue(dialog.contains("transferAllSourceImages"));
        assertTrue(css.contains(".technical-problem-image-resize-handle"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
