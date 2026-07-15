package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StudyProblemExportT107SourceTest {
    @Test
    void canvasHasPremiumExportOptionsWithSafeScaleAndContentCrop() throws Exception {
        String surface = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java");
        String options = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasExportOptions.java");
        String result = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasExportResult.java");

        assertTrue(surface.contains("StudyProblemCanvasExportResult exportWithImages"));
        assertTrue(surface.contains("copyBackgroundTiles(output, size.scale(), size.originX(), size.originY())"));
        assertTrue(surface.contains("private static ImageBounds imageBounds"));
        assertTrue(surface.contains("originX"));
        assertTrue(surface.contains("originY"));
        assertTrue(surface.contains("La exportacion bajo de"));
        assertFalse(surface.contains("SnapshotParameters"));
        assertTrue(options.contains("premiumExternal()"));
        assertTrue(options.contains("internalPersistence()"));
        assertTrue(options.contains("undoSnapshot()"));
        assertTrue(result.contains("WritableImage image"));
    }

    @Test
    void dialogSeparatesInternalPersistenceFromExternalPremiumPng() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java");
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/StudyProblemWorkflow.java");

        assertTrue(dialog.contains("externalCanvasSnapshot"));
        assertTrue(dialog.contains("StudyProblemCanvasExportOptions.internalPersistence()"));
        assertTrue(dialog.contains("StudyProblemCanvasExportOptions.premiumExternal()"));
        assertTrue(workspace.contains("exportExternalTechnicalProblemImage(result)"));
        assertTrue(viewModel.contains("exportTechnicalProblemImage(WritableImage image, Path target)"));
        assertTrue(workflow.contains("exportSolutionImage(WritableImage image, Path target)"));
        assertFalse(workspace.contains("viewModel.exportTechnicalProblemImage(problem.id(), result.externalPngTarget())"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
