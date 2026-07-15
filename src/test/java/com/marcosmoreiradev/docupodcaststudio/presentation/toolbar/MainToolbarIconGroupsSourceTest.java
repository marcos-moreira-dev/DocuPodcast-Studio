package com.marcosmoreiradev.docupodcaststudio.presentation.toolbar;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MainToolbarIconGroupsSourceTest {
    @Test
    void mainToolbarUsesIconGroupsThroughCommandDispatcherAndKeepsAdvancedWorkspacesOutOfPrimaryRow() throws Exception {
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String component = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ToolbarActionButton.java"));
        String css = Files.readString(Path.of("src/main/resources/css/toolbar.css"));

        assertTrue(toolbar.contains("ToolbarActionButton"));
        assertTrue(toolbar.contains("groupLabel(\"Documento\")"));
        assertTrue(toolbar.contains("groupLabel(\"Lectura\")"));
        assertTrue(toolbar.contains("groupLabel(\"Salida\")"));
        assertTrue(toolbar.contains("groupLabel(\"Vista\")"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.SHOW_WELCOME)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.OPEN_SOURCE_DOCUMENT)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.LISTEN_DOCUMENT)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.EXPORT_PODCAST_WAV)"));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.TOGGLE_FULLSCREEN)"));
        assertFalse(toolbar.contains("shellView::handleToggleFullScreen"),
                "T99B centraliza superficies visibles por AppCommandId; no debe volver el handler directo.");
        assertTrue(component.contains("icon + short text"));
        assertTrue(css.contains("ui-toolbar-action"));
        assertTrue(css.contains("toolbar con marcadores sobrios"));
        assertFalse(toolbar.contains("📄"));
        assertFalse(toolbar.contains("📦"));
        assertFalse(toolbar.contains("🎙"));
        assertFalse(toolbar.contains("🖼"));
        assertFalse(toolbar.contains("⌂"));
        assertFalse(toolbar.contains("⛶"));

        assertFalse(toolbar.contains("Narración avanzada"));
    }
}
