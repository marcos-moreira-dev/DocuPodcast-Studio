package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemHotfixT116BSourceTest {
    @Test
    void pdfRegionSelectionHasOverlayAndFrameFallback() throws Exception {
        String pdfView = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java");

        assertTrue(pdfView.contains("selectionOverlay.addEventFilter(MouseEvent.MOUSE_PRESSED"));
        assertTrue(pdfView.contains("frame.addEventFilter(MouseEvent.MOUSE_PRESSED"));
        assertTrue(pdfView.contains("imagePointFromEvent"));
        assertTrue(pdfView.contains("frame.sceneToLocal(event.getSceneX(), event.getSceneY())"));
    }

    @Test
    void eraserClearsInkOnlyAndUndoDoesNotRepaintBackground() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String surface = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");

        assertTrue(surface.contains("void eraseLine"));
        assertTrue(surface.contains("AlphaComposite.Clear"));
        assertTrue(surface.contains("refreshTileCanvasFromRaster"));
        assertTrue(dialog.contains("drawingSurface.previewLine"));
        assertTrue(dialog.contains("drawingSurface.commitInkStroke"));
        assertTrue(dialog.contains("drawingSurface.drawSnapshot(image)"));
        assertFalse(dialog.contains("eraser.isSelected() ? backgroundColor.getValue()"));
        assertFalse(dialog.contains("drawingSurface.drawSnapshot(image, backgroundColor.getValue())"));
    }

    @Test
    void hiddenStatementCanAlwaysBeRestoredFromResolverSide() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String css = read("src/main/resources/css/document/study-problem.css");

        assertTrue(dialog.contains("restoreStatementButton"));
        assertTrue(dialog.contains("\"Mostrar enunciado\""));
        assertTrue(dialog.contains("setNodeVisible(restoreStatementButton, statementCollapsed)"));
        assertTrue(dialog.contains("technical-problem-statement-actions"));
        assertTrue(css.contains(".technical-problem-statement-actions"));
        assertTrue(css.contains(".technical-problem-statement-restore"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
