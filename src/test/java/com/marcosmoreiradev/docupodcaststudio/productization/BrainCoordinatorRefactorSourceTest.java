package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BrainCoordinatorRefactorSourceTest {
    @Test
    void brainRefactorIntroducesCoordinatorsWithoutMovingFaceFirst() throws IOException {
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentIntakeCoordinator.java",
                "read-only source", "document-root workflow", "applyReadingProfile");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/SourceDocumentRefreshCoordinator.java",
                "Refrescar contenido", "read-only", "without deleting audio, layers or storyboard");
        assertFileContains("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/WorkspaceNavigationCoordinator.java",
                "does not mark", "replaceProjectPreservingDirty");
    }

    @Test
    void documentWorkspaceExposesRefreshThroughSharedActionFactory() throws IOException {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String floating = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        assertTrue(view.contains("Refrescar contenido"));
        assertTrue(view.contains("new FloatingReadingControlBar"));
        assertTrue(view.contains("viewModel::refreshSourceDocument"));
        assertTrue(floating.contains("ActionButtonFactory.transportIcon"));
    }

    private static void assertFileContains(String path, String... expected) throws IOException {
        String content = Files.readString(Path.of(path));
        for (String item : expected) {
            assertTrue(content.contains(item), path + " debe contener: " + item);
        }
    }
}
