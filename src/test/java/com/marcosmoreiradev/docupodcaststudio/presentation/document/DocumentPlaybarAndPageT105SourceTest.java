package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentPlaybarAndPageT105SourceTest {
    @Test
    void documentUsesFloatingReadingControlOverExpandedPage() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));

        assertTrue(source.contains("StackPane readingStage"));
        assertTrue(source.contains("document-reading-stage"));
        assertTrue(source.contains("StackPane.setAlignment(readingControls, Pos.TOP_CENTER)"));
        assertTrue(source.contains("StackPane.setMargin(readingControls"));
        assertTrue(source.contains("DOCUMENT_PAGE_MAX_WIDTH = 1280.0"));
        assertTrue(source.contains("documentModeLabel"));
        assertTrue(source.contains("\"Vista documento\""));
        assertTrue(source.contains("\"Mapa textual\""));
        assertTrue(css.contains("document-reading-stage"));
        assertTrue(css.contains("-fx-padding: 8 10 10 10"));
        assertTrue(css.contains("-fx-padding: 142 12 74 12"));
        assertTrue(css.contains("-fx-padding: 78 38 74 38"));
        assertTrue(source.contains("documentTitleWithExtension"));
        assertFalse(source.contains("documentSurface.setTop(floatingReadingControl())"));
    }

    @Test
    void welcomeDoesNotShowImplementationBadgesInTheCenter() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));

        assertFalse(source.contains("new InfoBadge(\"Local\""));
        assertFalse(source.contains("Fuente intacta"));
        assertFalse(source.contains("V1: lector + capas"));
        assertFalse(source.contains("Consejo: usa la barra superior"));
    }
}
