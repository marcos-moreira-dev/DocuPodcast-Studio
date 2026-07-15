package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DemoDocxAccentsSourceTest {
    @Test
    void aviadoresDemoSourceDocxKeepsSpanishAccents() throws IOException {
        String xml = documentXml();

        assertTrue(xml.contains("Gui\u00f3n teatral c\u00f3mico"));
        assertTrue(xml.contains("Capit\u00e1n Bigote"));
        assertTrue(xml.contains("avi\u00f3n antiguo"));
        assertTrue(xml.contains("viejo aer\u00f3dromo"));
        assertTrue(xml.contains("vuelo p\u00fablico"));
        assertTrue(xml.contains("esta m\u00e1quina"));
        assertTrue(xml.contains("La dignidad est\u00e1"));
        assertTrue(xml.contains("peque\u00f1a omisi\u00f3n"));
        assertTrue(xml.contains("\u00bfCu\u00e1l era?"));
        assertTrue(xml.contains("Entonces, \u00bffue un \u00e9xito?"));
        assertTrue(xml.contains("Y as\u00ed termin\u00f3"));
        assertTrue(xml.contains("de d\u00f3nde salieron"));

        assertFalse(xml.contains("Gui?n"));
        assertFalse(xml.contains("Guion teatral comico"));
        assertFalse(xml.contains("un avion"));
        assertFalse(xml.contains("mi capitan"));
    }

    private static String documentXml() throws IOException {
        Path docx = Path.of("src/main/resources/examples/aviadores-comicos/source.docx");
        try (ZipFile zip = new ZipFile(docx.toFile())) {
            return new String(zip.getInputStream(zip.getEntry("word/document.xml")).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
