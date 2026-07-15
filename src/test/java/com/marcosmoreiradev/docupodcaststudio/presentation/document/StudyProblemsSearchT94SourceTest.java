package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyProblemsSearchT94SourceTest {
    @Test
    void problemPanelContainsSearchFiltersCountAndSourceNavigation() throws Exception {
        String panel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java"));

        assertTrue(panel.contains("TextField search"));
        assertTrue(panel.contains("StudyProblemStatusFilter"));
        assertTrue(panel.contains("ChapterOption"));
        assertTrue(panel.contains("sourceSelector"));
        assertTrue(panel.contains("\"Ir a fuente\""));
        assertTrue(panel.contains("projection.totalProblemCount()"));
        assertTrue(panel.contains("viewModel.selectDocumentBlock(source.blockId())"));
    }

    @Test
    void problemPanelUsesTransversalControlsAndKeepsExistingSideDock() throws Exception {
        String panel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java"));
        String workspace = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String studyDock = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java"));
        String ids = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockModuleId.java"));

        assertTrue(panel.contains("ActionButtonFactory.primary"));
        assertTrue(panel.contains("ActionButtonFactory.secondary"));
        assertTrue(panel.contains("StudioFormControls.textInput"));
        assertTrue(panel.contains("StudioFormControls.combo"));
        assertTrue(workspace.contains("DocumentStudySideDock"));
        assertTrue(studyDock.contains("SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM"));
        assertFalse(ids.contains("DOCUMENT_STUDY_PROBLEMS"));
        assertFalse(ids.contains("SAVED_PROBLEMS"));
    }
}
