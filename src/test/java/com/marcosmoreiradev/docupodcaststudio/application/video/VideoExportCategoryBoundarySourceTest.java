package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoExportCategoryBoundarySourceTest {
    @Test
    void transversalDocumentaryAndNarrativePathsDoNotImportTheatreTypes() throws Exception {
        assertNoTheatreImports(read("application/video/RenderFinalVideoPlanUseCase.java"));
        assertNoTheatreImports(read("application/documentstudy/ExportDocumentStudyVideoUseCase.java"));
        assertNoTheatreImports(read("application/video/ExportNarrativeVideoUseCase.java"));
    }

    @Test
    void workflowUsesAnExplicitServiceForEveryProjectCategory() throws Exception {
        String workflow = read("presentation/shell/workflow/ExportWorkflowCoordinator.java");

        assertTrue(workflow.contains("exportDocumentStudyVideo()"));
        assertTrue(workflow.contains("exportNarrativeVideo()"));
        assertTrue(workflow.contains("exportTheatreVideo()"));
    }

    private static void assertNoTheatreImports(String source) {
        assertFalse(source.contains("import com.marcosmoreiradev.docupodcaststudio.application.theatre"));
        assertFalse(source.contains("import com.marcosmoreiradev.docupodcaststudio.domain.theatre"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio").resolve(relativePath));
    }
}
