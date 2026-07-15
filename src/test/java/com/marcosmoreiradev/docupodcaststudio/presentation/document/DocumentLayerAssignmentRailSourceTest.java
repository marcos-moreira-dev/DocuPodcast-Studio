package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentLayerAssignmentRailSourceTest {
    @Test
    void documentWorkspaceMovesFrequentLayerActionsToContextInspector() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String audioPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String imagePanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String mediaRail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String theatreDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"));

        assertFalse(workspace.contains("buildLayerRail"));
        assertFalse(workspace.contains("new CollapsibleMediaRail"));
        assertTrue(rail.contains("setExpanded"));
        assertTrue(rail.contains("SidePanelToggleButton"));
        assertTrue(rail.contains("DocumentSidePanelChrome.header"));
        assertTrue(rail.contains("Ocultar o mostrar el panel visual"));
        assertTrue(audioPanel.contains("Voz IA"));
        assertTrue(audioPanel.contains("Audio del computador"));
        assertTrue(audioPanel.contains("Elegir audio"));
        assertTrue(audioPanel.contains("Extraer video"));
        assertTrue(imagePanel.contains("Elegir imagen"));
        assertTrue(mediaRail.contains("Fragmentos visuales"));
        assertTrue(theatreDock.contains("DocumentImageContextPanel"));
        assertTrue(theatreDock.contains("DocumentMediaRailView"));
        assertFalse(mediaRail.contains("DocumentLayerRailView actions"));
        assertTrue(css.contains("ui-collapsible-rail"));
    }
}
