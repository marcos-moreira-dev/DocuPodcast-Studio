package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfTextRepairServiceTest {
    private final PdfTextRepairService service = new PdfTextRepairService();

    @Test
    void repairsSafeLetterSpacedWordsAndArtificialLineBreaks() {
        PdfTextNormalizationReport report = service.repair("""
                H O L A mundo
                Esta linea continua con una idea
                que sigue en minuscula.
                """);

        assertTrue(report.normalizedText().contains("HOLA mundo"));
        assertTrue(report.normalizedText().contains("Esta linea continua con una idea que sigue en minuscula."));
        assertEquals(1, report.letterSpacingRepairs());
        assertEquals(1, report.artificialLineBreakRepairs());
    }

    @Test
    void doesNotRepairFormulaCodeOrTableLikeLinesAggressively() {
        PdfTextNormalizationReport report = service.repair("""
                A B C | D E F
                x = a + b / c
                O(n) queda igual.
                """);

        assertTrue(report.normalizedText().contains("A B C | D E F"));
        assertTrue(report.normalizedText().contains("x = a + b / c"));
        assertFalse(report.normalizedText().contains("ABC | DEF"));
    }

    @Test
    void removesRepeatedShortHeadersOnlyWhenClearlyRepeated() {
        PdfTextNormalizationReport report = service.repair("""
                Documento de prueba
                Contenido uno.
                Documento de prueba
                Contenido dos.
                Documento de prueba
                Contenido tres.
                """);

        assertFalse(report.normalizedText().contains("Documento de prueba"));
        assertEquals(3, report.repeatedHeaderFooterLinesRemoved());
    }
}
