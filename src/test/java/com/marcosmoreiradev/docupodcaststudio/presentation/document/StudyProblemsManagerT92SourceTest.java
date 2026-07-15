package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyProblemsManagerT92SourceTest {
    @Test
    void problemPanelContainsSavedProblemsManager() throws Exception {
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentTechnicalProblemPanel.java"));

        assertTrue(panel.contains("Problemas guardados"));
        assertTrue(panel.contains("ListView<StudyProblemListItem>"));
        assertTrue(panel.contains("Abrir / editar solucion"));
        assertTrue(panel.contains("Exportar PNG"));
        assertTrue(panel.contains("Exportar texto"));
        assertTrue(panel.contains("deleteTechnicalProblem"));
    }

    @Test
    void documentWorkspaceKeepsSingleProblemSideDockModule() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String studyDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentStudySideDock.java"));
        String ids = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockModuleId.java"));

        assertTrue(workspace.contains("DocumentStudySideDock"));
        assertTrue(studyDock.contains("SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM"));
        assertEquals(1, count(studyDock, "SideDockModuleId.DOCUMENT_TECHNICAL_PROBLEM"));
        assertFalse(ids.contains("DOCUMENT_STUDY_PROBLEMS"));
        assertFalse(ids.contains("SAVED_PROBLEMS"));
    }

    @Test
    void technicalProblemDialogSupportsEditMode() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java"));

        assertTrue(dialog.contains("showForEdit"));
        assertTrue(dialog.contains("StudyProblemDetail"));
        assertTrue(dialog.contains("title.setEditable(false)"));
        assertTrue(dialog.contains("notes"));
    }

    private static int count(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
