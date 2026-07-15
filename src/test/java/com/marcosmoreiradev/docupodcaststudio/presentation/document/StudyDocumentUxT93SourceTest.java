package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyDocumentUxT93SourceTest {
    @Test
    void technicalProblemDialogUsesStyledModesTooltipsAndDynamicPanning() throws Exception {
        String dialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java"));

        assertTrue(dialog.contains("dialog.setResizable(true)"));
        assertTrue(dialog.contains("StudioFormControls.toggle(\"Lienzo/texto: lienzo\""));
        assertTrue(dialog.contains("Panear/dibujar: dibujar"));
        assertTrue(dialog.contains("updateCanvasScrollMode"));
        assertTrue(dialog.contains("ScrollPane.ScrollBarPolicy.NEVER"));
        assertFalse(dialog.contains("scroll.setPannable(true)"));
        assertTrue(dialog.contains("handleDialogKeyPressed"));
        assertTrue(dialog.contains("KeyCode.Z"));
        assertTrue(dialog.contains("KeyCode.Y"));
        assertTrue(dialog.contains("textInputOwnsShortcut"));
        assertTrue(dialog.contains("LucideIconView.of(iconName)"));
        assertTrue(dialog.contains("iconToolButton(\"undo-2\""));
        assertTrue(dialog.contains("iconToolButton(\"redo-2\""));
    }

    @Test
    void documentStudyProblemPanelLivesInRightDock() throws Exception {
        String workspace = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String studyDock = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java"));

        assertTrue(workspace.contains("buildDocumentStudySideDock()"));
        assertTrue(studyDock.contains("WorkspaceSideDock.RailPlacement.RIGHT"));
        assertTrue(workspace.contains("StackPane rightDockHost = new StackPane(documentStudySideDock, theatreSideDock)"));
        assertTrue(workspace.contains("rightDockHost.prefWidthProperty().bind"));
        assertTrue(workspace.contains("SplitPane.setResizableWithParent(rightDockHost, Boolean.FALSE)"));
        assertTrue(workspace.contains("currentProjectUsesTheatreDock()"));
        assertTrue(workspace.contains("new SplitPane(leftDock, documentSurface, rightDockHost)"));
    }
}
