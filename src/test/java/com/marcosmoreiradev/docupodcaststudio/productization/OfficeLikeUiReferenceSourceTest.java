package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class OfficeLikeUiReferenceSourceTest {
    @Test
    void officeLikeReferenceIsInspirationNotFullWordProcessorScope() throws Exception {
        String reference = read("docs/productizacion/REFERENCIA_OFIMATICA_WORD_LIKE.md");
        String validation = read("VALIDATION.md");
        Path screenshot = Path.of("docs/referencias/ui/wps-office-wordlike-reference-2026-05-30.png");

        assertTrue(Files.exists(screenshot), "la captura de referencia debe quedar archivada");
        assertTrue(Files.size(screenshot) > 1024, "la captura de referencia no debe estar vacia");

        assertTrue(reference.contains("no ordena copiar Microsoft Word, WPS Office ni ningún editor de texto"));
        assertTrue(reference.contains("página blanca centrada"));
        assertTrue(reference.contains("barra superior con grupos de acciones por intención"));
        assertTrue(reference.contains("DocuPodcast no debe convertirse en procesador de texto completo"));
        assertTrue(reference.contains("inspiración de lectura sí"));
        assertTrue(reference.contains("edición ofimática completa no"));

        assertTrue(validation.contains("inspiración Word/WPS no equivale a editor ofimático completo"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
