package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentRightRailT107SourceTest {
    @Test
    void rightRailIsCollapsibleWithoutVisibleSplitBarAndVisualOnly() throws Exception {
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java"));
        String mediaRail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"))
                + Files.readString(Path.of("src/main/resources/css/compat-legacy.css"));

        assertTrue(rail.contains("ui-rail-resize-handle"));
        assertTrue(rail.contains("setMouseTransparent(true)"));
        assertFalse(rail.contains("Cursor.H_RESIZE"));
        assertFalse(rail.contains("MIN_EXPANDED_WIDTH"));
        assertFalse(rail.contains("MAX_EXPANDED_WIDTH"));
        assertFalse(css.contains("h-resize"));
        assertTrue(css.contains(".document-split > .split-pane-divider"));
        assertTrue(css.contains("-fx-pref-width: 0"));
        assertTrue(mediaRail.contains("removeAllDocumentImages"));
        assertTrue(mediaRail.contains("Eliminar todas las imágenes asignadas"));
        assertFalse(mediaRail.contains("Elegir imagen"), "la asignacion sigue viviendo en el sidebar izquierdo");
    }

    @Test
    void imageAndVoiceFlowsAutomateObviousSteps() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String imagePanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String audioPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(shell.contains("new DocumentWorkspaceView(viewModel, this::handleSaveProject,"));
        assertTrue(workspace.contains("BooleanSupplier saveProjectRequest"));
        assertTrue(imagePanel.contains("saveProjectRequest.getAsBoolean()"));
        assertTrue(imagePanel.contains("importSelectedImage(file, kind)"));
        assertTrue(audioPanel.contains("Ir a biblioteca de voces"));
        assertTrue(audioPanel.contains("showVoiceLibraryWorkspace"));
        assertTrue(viewModel.contains("removeAllDocumentImages"));
        assertTrue(viewModel.contains("deleteProjectAssetFileIfPresent"));
    }
}
