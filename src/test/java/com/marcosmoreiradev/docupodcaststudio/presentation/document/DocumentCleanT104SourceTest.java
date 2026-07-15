package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentCleanT104SourceTest {
    @Test
    void documentWorkspaceUsesCenteredReadingPageAndStableScroll() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));

        assertTrue(source.contains("StackPane pageHost"));
        assertTrue(source.contains("DOCUMENT_PAGE_MAX_WIDTH"));
        assertTrue(source.contains("ACTIVE_READING_TOP_OFFSET"));
        assertTrue(source.contains("scrollNodeNearReadingTop"));
        assertTrue(source.contains("localToScene"));
        assertFalse(source.contains("documentScroll.setVvalue(index / denominator)"));
        assertTrue(css.contains("T105 — pagina expandida"));
        assertTrue(css.contains("document-page-host"));
        assertTrue(css.contains("-fx-padding: 78 38 74 38"));
        assertTrue(source.contains("documentTitleWithExtension"));
        assertTrue(source.contains("document-reading-stage"));
        assertTrue(source.contains("StackPane.setMargin(readingControls"));
    }

    @Test
    void documentWorkspaceKeepsReaderLanguageHumanAndNonTechnical() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));

        assertTrue(source.contains("\"Vista documento\""));
        assertTrue(source.contains("\"Mapa textual\""));
        assertTrue(source.contains("documentModeLabel"));
        assertFalse(source.contains("Fuente solo lectura"));
        assertFalse(source.contains("Refrescar contenido actualiza la copia del proyecto"));
        assertFalse(source.contains("listeningFlowStatus"));
        assertTrue(source.contains("no dentro del Word original"));
        assertFalse(source.contains("Formato: %s · Bloques"));
        assertFalse(source.contains("document-block-kind"));
        assertFalse(source.contains("Label kind = new Label"));
        assertTrue(css.contains("document-mode-label"));
        assertTrue(css.contains("document-theatre-text-alias"));
        assertTrue(css.contains("document-theatre-replica-id"));
        assertTrue(css.contains("document-block-theatre-scene-focus"));
        assertTrue(css.contains("document-sentence-selected"));
    }
}
