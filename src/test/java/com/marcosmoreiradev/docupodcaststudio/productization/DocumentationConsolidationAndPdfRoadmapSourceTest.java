package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the current documentation authority, refactor criteria and future PDF scope. */
final class DocumentationConsolidationAndPdfRoadmapSourceTest {
    @Test
    void currentDocumentationDeclaresSingleOperationalAuthorityAndHistoricalArchive() throws Exception {
        String readme = read("DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md");

        assertTrue(readme.contains("única fuente de verdad operativa"));
        assertTrue(readme.contains("archivo histórico"));
        assertTrue(readme.contains("docs/"));
        assertTrue(readme.contains("DOCUMENTACION_ESTRATEGICA/"));
        assertTrue(readme.contains("00_MEMORIA_PROYECTO/"));
    }

    @Test
    void roadmapKeepsPdfAcceptedButAdvancedSupportFuture() throws Exception {
        String roadmap = read("DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md");

        assertTrue(roadmap.contains("PDF ya es un formato aceptado"));
        assertTrue(roadmap.contains("Word/DOCX"));
        assertTrue(roadmap.contains("PDF avanzado"));
        assertTrue(roadmap.contains("PDFs escaneados"));
        assertFalse(roadmap.contains("implementar OCR ahora"));
    }

    @Test
    void refactorDebtDocumentsSolidCohesionAndTransversalBoundaries() throws Exception {
        String debt = read("DOCUMENTACION_ACTUAL/05_DEUDA_TECNICA_Y_REFACTOR.md");

        assertTrue(debt.contains("Favorecer cohesión"));
        assertTrue(debt.contains("SOLID"));
        assertTrue(debt.contains("procesos externos, rutas embebidas, diagnóstico, settings"));
        assertTrue(debt.contains("XTTS, Piper, FFmpeg/video, lectura, playback, voces y proyectos"));
        assertTrue(debt.contains("No crear abstracciones especulativas"));
    }

    @Test
    void touchedCurrentDocsRemainUtf8WithoutMojibake() throws Exception {
        String docs = read("DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md")
                + read("DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md")
                + read("DOCUMENTACION_ACTUAL/05_DEUDA_TECNICA_Y_REFACTOR.md");

        assertTrue(docs.contains("Documentación"));
        assertTrue(docs.contains("implementación"));
        assertTrue(docs.contains("técnica"));
        assertFalse(docs.contains("Ã"));
        assertFalse(docs.contains("â€”"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
