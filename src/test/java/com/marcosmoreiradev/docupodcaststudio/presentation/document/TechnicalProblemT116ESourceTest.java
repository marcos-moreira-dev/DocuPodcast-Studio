package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TechnicalProblemT116ESourceTest {
    @Test
    void canvasGrowthBacksOffAndCanRepeatWithoutBlockingControls() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String surface = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");

        assertTrue(dialog.contains("suppressCanvasGrowEvents"));
        assertTrue(dialog.contains("canvasScroll.setHvalue(0.78)"));
        assertTrue(dialog.contains("canvasScroll.setVvalue(0.78)"));
        assertTrue(dialog.contains("Platform.runLater(() -> suppressCanvasGrowEvents = false)"));
        assertTrue(surface.contains("SCROLL_GROWTH_FACTOR = 1.25"));
        assertTrue(surface.contains("MAX_CANVAS_COLUMNS = 6"));
        assertTrue(surface.contains("Math.max(beforeWidth + 1.0, beforeWidth * SCROLL_GROWTH_FACTOR)"));
        assertTrue(surface.contains("Math.max(beforeHeight + 1.0, beforeHeight * SCROLL_GROWTH_FACTOR)"));
    }

    @Test
    void drawingUsesImmediateLinePreviewAndKeepsLegacyCurveReplay() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String surface = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");
        String engine = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ink/InkRealtimeStrokeEngine.java");

        assertTrue(dialog.contains("InkRealtimeStrokeEngine"));
        assertTrue(dialog.contains("drawingSurface.commitInkStroke"));
        assertTrue(engine.contains("previewSegment"));
        assertTrue(engine.contains("previewQuadratic"));
        assertTrue(engine.contains("FRAME_BUDGET_NANOS"));
        assertTrue(surface.contains("QuadCurve2D"));
        assertTrue(surface.contains("eraseQuadratic"));
        assertTrue(surface.contains("AlphaComposite.Clear"));
        String newStrokePath = surface.substring(surface.indexOf("private List<InkCommand> commandsForStroke"),
                surface.indexOf("private static double widthForPoint"));
        assertFalse(newStrokePath.contains("InkCommand.quadratic"));
        assertTrue(newStrokePath.contains("InkCommand.line"));
    }

    @Test
    void documentSideDocksStartCollapsedWhenProjectChanges() throws Exception {
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");

        assertTrue(viewModel.contains("documentRightRailVisible = new SimpleBooleanProperty(false)"));
        assertTrue(viewModel.contains("resetDocumentSideDocksForProjectStart()"));
        assertTrue(viewModel.contains("documentRightRailVisible.set(false)"));
        assertTrue(viewModel.contains("documentPlaybarDocked.set(false)"));
        assertTrue(workspace.contains("WorkspaceSideDock.RailPlacement.LEFT"));
        assertTrue(workspace.contains("registry,\n                true,\n                WorkspaceSideDock.RailPlacement.LEFT"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
