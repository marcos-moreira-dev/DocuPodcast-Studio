package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSidePanelChromeRf15SourceTest {
    @Test
    void documentSidebarsUseSharedChromeAndIconToggle() throws Exception {
        String sideDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java");
        String rightRail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java");
        String toggle = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SidePanelToggleButton.java");
        String chrome = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/DocumentSidePanelChrome.java");
        String appStyles = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java");

        assertTrue(sideDock.contains("DocumentSidePanelChrome.header"));
        assertTrue(rightRail.contains("DocumentSidePanelChrome.header"));
        assertTrue(sideDock.contains("new SidePanelToggleButton"));
        assertTrue(rightRail.contains("new SidePanelToggleButton"));
        assertFalse(sideDock.contains("sideDockHide(\"X\""));
        assertFalse(rightRail.contains("RailToggleButton(\"<\""));

        assertTrue(toggle.contains("IconView.rail(safeIcon)"));
        assertTrue(toggle.contains("setAccessibleText(safeTooltip)"));
        assertTrue(toggle.contains("new Tooltip(safeTooltip)"));
        assertTrue(chrome.contains("UI_DOCUMENT_SIDE_PANEL_HEADER"));
        assertTrue(appStyles.contains("UI_SIDE_PANEL_TOGGLE_BUTTON"));
    }

    @Test
    void documentSidebarTitlesAndVisualAccentArePreserved() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String theatreDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java");
        String mediaRail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String css = read("src/main/resources/css/compat-legacy.css");

        assertTrue(workspace.contains("\"Fragmento\""));
        assertTrue(theatreDock.contains("Capas multimedia"));
        assertTrue(mediaRail.contains("Fragmentos visuales"));
        assertTrue(css.contains(".document-side-panel-header"));
        assertTrue(css.contains(".document-side-panel-toggle"));
        assertTrue(css.contains(".workspace-side-dock-right .side-dock-rail"));
        assertTrue(css.contains(".theatre-fragment-images-split"));
        assertTrue(css.contains("#D5E1EF"));
        assertTrue(css.contains("#7EA2D6"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
