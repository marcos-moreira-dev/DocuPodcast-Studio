package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReaderUiRedesignSourceTest {
    @Test
    void documentReaderKeepsTechnicalCockpitOutOfMainPage() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/document/document-page.css"));
        String docs = Files.readString(Path.of("docs/productizacion/REDISENO_UI_APLICADO_T68.md"));

        assertTrue(document.contains("\"Vista documento\""));
        assertTrue(document.contains("\"Mapa textual\""));
        assertFalse(document.contains("Fuente solo lectura"));
        assertFalse(document.contains("Refrescar contenido actualiza la copia del proyecto"));
        assertFalse(document.contains("Pantalla operativa simple"));
        assertFalse(document.contains("Formato: %s · Bloques"));
        assertFalse(document.contains("texto narrable"));
        assertFalse(document.contains("estilo Word:"));
        assertTrue(welcome.contains("Word/DOCX, PDF con texto u OCR local, Markdown o TXT"));
        assertTrue(welcome.contains("DOCX, PDF con texto u OCR local, Markdown o TXT"));
        assertTrue(css.contains("document-mode-label"));
        assertFalse(document.contains("document-block-kind"));
        assertFalse(document.contains("Label kind = new Label"));
        assertTrue(docs.contains("no como una cabina técnica"));
    }
}
