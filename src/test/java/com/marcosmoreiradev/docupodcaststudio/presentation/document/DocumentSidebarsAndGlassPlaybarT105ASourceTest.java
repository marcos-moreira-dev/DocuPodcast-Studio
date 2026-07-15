package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSidebarsAndGlassPlaybarT105ASourceTest {
    @Test
    void playbarIsTopPinnedGlassAndDoesNotStretchOverThePage() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String floating = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java");
        String css = read("src/main/resources/css/components/actions.css")
                + read("src/main/resources/css/document/document-page.css");

        assertTrue(workspace.contains("StackPane.setAlignment(readingControls, Pos.TOP_CENTER)"));
        assertTrue(workspace.contains("new Insets(6, 24, 0, 24)"));
        assertTrue(floating.contains("setMaxHeight(Region.USE_PREF_SIZE)"));
        assertTrue(floating.contains("document-reading-glass-bar"));
        assertTrue(css.contains("rgba(31, 41, 55, 0.42)"));
    }

    @Test
    void documentSidebarsAreLessVerboseAndUseCompactControls() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String sideDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java");
        String context = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java");
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String theatreDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");
        String collapsible = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java");

        assertFalse(workspace.contains("Fragmentos con imagen"));
        assertTrue(theatreDock.contains("Capas multimedia"));
        assertFalse(sideDock.contains("sideDockHide(\"X\""));
        assertTrue(sideDock.contains("DocumentSidePanelChrome.header"));
        assertTrue(sideDock.contains("SidePanelToggleButton"));
        assertTrue(context.contains("Fragmento"));
        assertTrue(context.contains("La fuente original no se edita"));
        assertFalse(rail.contains("Asignadas"));
        assertTrue(rail.contains("Fragmentos visuales"));
        assertFalse(rail.contains("railTitle(\"Imágenes\")"));
        assertTrue(collapsible.contains("DocumentSidePanelChrome.header"));
        assertTrue(collapsible.contains("SidePanelToggleButton"));
        assertFalse(collapsible.contains("RailToggleButton(\"<\""));
        assertFalse(context.contains("metadatos"));
        assertFalse(rail.contains("Sin medios asignados todavía."));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
