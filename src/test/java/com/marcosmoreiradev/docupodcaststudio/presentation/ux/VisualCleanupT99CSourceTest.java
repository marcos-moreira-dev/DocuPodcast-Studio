package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T99C guardrail: the temporary GUI cleanup must not reintroduce visible visual scaffolding. */
final class VisualCleanupT99CSourceTest {
    @Test
    void documentReaderDoesNotExposeTruncatedTabsOrBlockTypeLabelsInThePage() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(document.contains("\"Texto\""));
        assertTrue(document.contains("\"Audio\""));
        assertTrue(document.contains("\"Imagen\""));
        assertFalse(document.contains("\"Det\""));
        assertFalse(document.contains("\"Aud\""));
        assertFalse(document.contains("\"Img\""));
        assertFalse(document.contains("document-block-kind"));
        assertFalse(document.contains("Label kind = new Label"));
        assertFalse(document.contains("Pantalla operativa simple"));
        assertFalse(document.contains("Lee el documento y escúchalo desde esta pantalla"));
    }

    @Test
    void toolbarUsesSoberTextMarkersInsteadOfEmojiPlaceholders() throws Exception {
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/toolbar.css"));

        assertTrue(toolbar.contains("toolbarButton(\"DOC\""));
        assertTrue(toolbar.contains("toolbarButton(\"PLAY\""));
        assertTrue(toolbar.contains("toolbarButton(\"OUT\""));
        assertTrue(toolbar.contains("shellView.dispatchCommand(AppCommandId.LISTEN_DOCUMENT)"));
        assertTrue(css.contains("toolbar con marcadores sobrios"));
        assertFalse(toolbar.contains("📄"));
        assertFalse(toolbar.contains("📦"));
        assertFalse(toolbar.contains("🔊"));
        assertFalse(toolbar.contains("🎙"));
        assertFalse(toolbar.contains("🖼"));
        assertFalse(toolbar.contains("📖"));
        assertFalse(toolbar.contains("⌂"));
        assertFalse(toolbar.contains("⛶"));
    }
}
