package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TableNarrationTextBuilderTest {
    private final TableNarrationTextBuilder builder = new TableNarrationTextBuilder();

    @Test
    void buildsStructuredTableReadingFromHeadersAndRows() {
        DocumentBlock table = DocumentBlock.of("B010", DocumentBlockType.TABLE_NOTICE, "Tabla", "table", Map.of(
                "table.rowCount", "3",
                "table.columnCount", "2",
                "table.header.0", "Algoritmo académico",
                "table.header.1", "Analogía administrativa",
                "table.cell.1.0", "key",
                "table.cell.1.1", "Nuevo producto",
                "table.cell.2.0", "Zona ordenada",
                "table.cell.2.1", "Lista por fecha"
        ));

        assertEquals("Lectura de cuadro. Encabezados: Algoritmo académico; Analogía administrativa. "
                        + "Fila 1. Algoritmo académico: key. Analogía administrativa: Nuevo producto. "
                        + "Fila 2. Algoritmo académico: Zona ordenada. Analogía administrativa: Lista por fecha.",
                builder.structuredText(table));
    }

    @Test
    void summaryKeepsTableCountsOnly() {
        DocumentBlock table = DocumentBlock.of("B010", DocumentBlockType.TABLE_NOTICE, "Tabla con mucho contenido", "table", Map.of(
                "rows", "5",
                "columns", "2"
        ));

        assertEquals("Tabla del documento fuente de 5 filas y 2 columnas.", builder.summaryText(table));
    }
}
