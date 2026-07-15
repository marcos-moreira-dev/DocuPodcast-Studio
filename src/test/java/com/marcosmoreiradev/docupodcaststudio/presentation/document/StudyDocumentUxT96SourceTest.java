package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyDocumentUxT96SourceTest {
    @Test
    void studyDockIsClippedScrollableAndMovesPlaybarToLeftRail() throws Exception {
        String shell = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String workspace = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String studyDock = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java"));

        assertTrue(shell.contains("installWorkspaceHostClip()"));
        assertTrue(shell.contains("workspaceHost.setClip(clip)"));
        assertTrue(workspace.contains("boolean dockedInLeftRail = rightSidebarExpanded"));
        assertTrue(workspace.contains("WorkspaceSideDock.RailPlacement.LEFT"));
        assertTrue(workspace.contains("this::railReadingControl"));
        assertTrue(studyDock.contains("scrollableProblemPanel"));
        assertTrue(studyDock.contains("new ScrollPane(panel)"));
        assertTrue(studyDock.contains("ScrollPane.ScrollBarPolicy.NEVER"));
        assertTrue(studyDock.contains("ScrollPane.ScrollBarPolicy.AS_NEEDED"));
    }

    @Test
    void technicalProblemDialogHasResponsiveFocusModeAndExternalPngExport() throws Exception {
        String dialog = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java"));

        assertTrue(dialog.contains("FlowPane"));
        assertTrue(dialog.contains("technical-problem-tools-flow"));
        assertTrue(dialog.contains("KeyCode.ESCAPE"));
        assertTrue(dialog.contains("Salir de pantalla completa"));
        assertTrue(dialog.contains("FileChooser"));
        assertTrue(dialog.contains("Guardar + exportar PNG..."));
        assertTrue(dialog.contains("externalPngTarget"));
        assertTrue(dialog.contains("chooseExternalPngTarget"));
        assertTrue(dialog.contains("ensureCanvasTilesCoverViewport"));
        assertTrue(dialog.contains("snapshotWithImages"));
    }
}
