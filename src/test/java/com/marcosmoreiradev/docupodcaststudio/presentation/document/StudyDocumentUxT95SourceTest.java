package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyDocumentUxT95SourceTest {
    @Test
    void studyProblemDockUsesTheatreLikeRightDockShell() throws Exception {
        String workspace = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String dock = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java"));

        assertTrue(workspace.contains("DocumentStudySideDock documentStudySideDock"));
        assertTrue(workspace.contains("new DocumentStudySideDock("));
        assertTrue(dock.contains("WorkspaceSideDock.RailPlacement.RIGHT"));
        assertTrue(dock.contains("documentRightRailVisibleProperty().addListener"));
        assertTrue(dock.contains("SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM"));
    }

    @Test
    void technicalProblemDialogHasResizableCanvasAndImageInteractionMode() throws Exception {
        String dialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java"));
        String canvas = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java"));

        assertTrue(dialog.contains("new SplitPane(statementNode, resolverNode)"));
        assertTrue(dialog.contains("toggleStatementCollapsed"));
        assertTrue(dialog.contains("toggleResolverFullscreen"));
        assertTrue(dialog.contains("ScrollPane.ScrollBarPolicy.NEVER"));
        assertTrue(dialog.contains("StudyProblemCanvasSurface"));
        assertTrue(canvas.contains("MAX_CANVAS_COLUMNS"));
        assertTrue(canvas.contains("MAX_CANVAS_ROWS"));
        assertTrue(dialog.contains("transferSourceImage"));
        assertTrue(dialog.contains("Interactuar con imagenes"));
        assertTrue(dialog.contains("resizeSelectedImage"));
        assertTrue(dialog.contains("deleteSelectedImage"));
        assertTrue(dialog.contains("snapshotDrawingCanvas"));
    }
}
