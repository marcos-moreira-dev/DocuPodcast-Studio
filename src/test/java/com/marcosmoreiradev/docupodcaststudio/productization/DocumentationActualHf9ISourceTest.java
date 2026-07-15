package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** HF9I guardrail: cierre del proyecto debe tener documentación vigente separada de la histórica. */
final class DocumentationActualHf9ISourceTest {
    @Test
    void currentDocumentationFolderIsTheCanonicalClosingEntryPoint() throws Exception {
        Path root = Path.of("DOCUMENTACION_ACTUAL");
        assertTrue(Files.isDirectory(root));
        assertContains(root.resolve("00_LEEME_PRIMERO.md"), "fuente documental vigente");
        assertContains(root.resolve("01_REGISTRO_TANDAS_RECIENTES.md"), "DOC-UX-HF9I");
        assertContains(root.resolve("03_ROADMAP_PENDIENTE_CIERRE.md"), "DOC-PERF-HF10");
        assertContains(root.resolve("06_DESCARGAS_MOTORES.md"), "URLs configurables");
    }

    @Test
    void rootDocsPointToCurrentDocumentationWithoutDeletingHistoricalFolders() throws Exception {
        assertContains(Path.of("README.md"), "DOCUMENTACION_ACTUAL/");
        assertContains(Path.of("AI_HANDOFF.md"), "DOCUMENTACION_ACTUAL/");
        assertContains(Path.of("VALIDATION.md"), "DOCUMENTACION_ACTUAL/");
        assertTrue(Files.exists(Path.of("docs/00_AVISO_DOCUMENTACION_HISTORICA.md")));
        assertTrue(Files.exists(Path.of("DOCUMENTACION/00_AVISO_DOCUMENTACION_HISTORICA.md")));
    }

    private static void assertContains(Path path, String expected) throws Exception {
        assertTrue(Files.exists(path), path + " debe existir");
        assertTrue(Files.readString(path).contains(expected), path + " debe contener: " + expected);
    }
}
