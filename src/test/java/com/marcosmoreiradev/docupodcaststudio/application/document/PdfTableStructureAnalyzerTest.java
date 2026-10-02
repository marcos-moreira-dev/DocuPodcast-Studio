package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;

import static org.junit.jupiter.api.Assertions.*;

final class PdfTableStructureAnalyzerTest {
    private final PdfTableStructureAnalyzer analyzer =
            new PdfTableStructureAnalyzer();

    @Test
    void classifiesNumericTableAndCalculatesOnlyCellBackedStatistics() {
        PdfRegion source = region("""
                Muestra | Temperatura °C | Presión kPa
                A | -2.5 | 1.0e2
                B | 0 | 1.5e2
                C | 3.5 | 1.2e2
                """);

        PdfTableStructure table = analyzer.analyze(
                1, List.of(source), Map.of("caption", "Ensayo"));
        PdfTableStatistics statistics = analyzer.statistics(table);

        assertEquals(PdfTableKind.NUMERIC, table.kind());
        assertEquals(4, table.rows());
        assertEquals(3, table.columns());
        assertTrue(table.units().stream().anyMatch(
                value -> value.equalsIgnoreCase("kPa")));
        assertEquals(2, statistics.columns().size());
        assertEquals(-2.5, statistics.columns().getFirst().minimum());
        assertEquals(3.5, statistics.columns().getFirst().maximum());
        assertEquals(PdfTableStatistics.ColumnStatistics.Trend.INCREASING,
                statistics.columns().getFirst().trend());
        assertTrue(statistics.numericCellValues().keySet().contains(
                statistics.columns().getFirst().minimumCellId()));
    }

    @Test
    void distinguishesProseKeyValueAndMatrix() {
        PdfTableStructure prose = analyzer.analyze(1, List.of(region("""
                Concepto | Explicación
                Poda | Elimina ramas sin solución
                Cota | Limita la mejor solución posible
                """)), Map.of());
        PdfTableStructure matrix = analyzer.analyze(1, List.of(region("""
                1 | 0 | -2
                0 | 1 | 3
                """)), Map.of());

        assertEquals(PdfTableKind.KEY_VALUE, prose.kind());
        assertEquals(PdfTableKind.MATRIX, matrix.kind());
    }

    @Test
    void narrationIsDraftAndNeverChangesCanonicalRegion() throws Exception {
        PdfRegion source = region("""
                Año | Casos
                2024 | 10
                2025 | 20
                """);
        PreparedPdfPage page = new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1,
                List.of(source), List.of(), "");
        AcademicTableNarrationEngine engine =
                new AcademicTableNarrationEngine();
        PdfDerivedTreatment result = engine.generate(page, List.of(source),
                new PdfDerivedTreatmentGenerationRequest(
                        java.nio.file.Path.of(".").toAbsolutePath(),
                        1, PdfDerivedTreatmentKind.TABLE_NARRATION,
                        List.of(source.id()), AcademicTableNarrationEngine.ID,
                        true, Map.of()));

        assertEquals(PdfDerivedTreatmentState.DRAFT, result.state());
        assertEquals("false",
                result.metadata().get("canonicalTextModified"));
        assertTrue(result.metadata().get("tableStructureJson")
                .contains("\"cells\""));
        assertEquals("""
                Año | Casos
                2024 | 10
                2025 | 20""", source.text());
    }

    @Test
    void manualCellEditsRemainDerivedAndKeepStableCellIds() {
        PdfRegion source = region("""
                Año | Cas0s
                2024 | 1O
                """);
        PdfTableStructure automatic = analyzer.analyze(
                1, List.of(source), Map.of());
        PdfTableStructure corrected = analyzer.analyze(
                1, List.of(source), Map.of("editedTableText", """
                        Año | Casos
                        2024 | 10
                        """));

        assertEquals(automatic.cells().stream().map(PdfTableCell::id).toList(),
                corrected.cells().stream().map(PdfTableCell::id).toList());
        assertEquals("Casos", corrected.cells().get(1).text());
        assertEquals("10", corrected.cells().get(3).text());
        assertEquals("true", corrected.metadata().get("manualCellEdit"));
        assertEquals("Año | Cas0s\n2024 | 1O", source.text());
    }

    @Test
    void distinguishesInferredObservedAndManualMergedCellGeometry() {
        PdfRegion source = region("""
                Grupo | Valor | Unidad
                A | 10 | kg
                B | 20 | kg
                """);
        PdfTableStructure inferred = analyzer.analyze(
                1, List.of(source), Map.of());
        PdfTableStructure observed = analyzer.analyze(
                1, List.of(source), Map.of(
                        "observedCellGeometry", "1,1,1,2,10,10,330,40"));
        PdfTableStructure manual = analyzer.analyze(
                1, List.of(source), Map.of(
                        "manualCellGeometry", "1,1,1,2,12,11,332,42"));

        assertTrue(inferred.cells().stream().allMatch(cell ->
                cell.geometryEvidence() == PdfTableGeometryEvidence.INFERRED));
        assertEquals("INFERRED", inferred.metadata().get("geometryEvidence"));
        assertEquals(PdfTableGeometryEvidence.OBSERVED,
                observed.cells().getFirst().geometryEvidence());
        assertEquals(2, observed.cells().getFirst().columnSpan());
        assertEquals(8, observed.cells().size(),
                "the covered cell must not be duplicated");
        assertEquals(PdfTableGeometryEvidence.MANUAL,
                manual.cells().getFirst().geometryEvidence());
        assertEquals("true", manual.metadata().get("manualGeometryEdit"));
        assertEquals("Grupo | Valor | Unidad\nA | 10 | kg\nB | 20 | kg",
                source.text());
    }

    @Test
    void fiveTablePoliciesProduceDistinctSafeNarrations() throws Exception {
        PdfRegion source = region("""
                Variable | Mínimo | Máximo
                alfa | 10 | 15
                beta | 20 | 25
                """);
        PreparedPdfPage page = new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1,
                List.of(source), List.of(), "");
        RuleBasedSmallTableNarrationEngine engine =
                new RuleBasedSmallTableNarrationEngine();
        java.util.function.Function<String, PdfDerivedTreatment> generate = policy -> {
            try {
                return engine.generate(page, List.of(source),
                        new PdfDerivedTreatmentGenerationRequest(
                                java.nio.file.Path.of(".").toAbsolutePath(), 1,
                                PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION,
                                List.of(source.id()), engine.id(), true,
                                Map.of("narrationPolicy", policy)));
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            }
        };

        assertTrue(generate.apply("READ_ALL").derivedText().contains("Fila 2"));
        assertFalse(generate.apply("READ_TEXTUAL_CONTENT").derivedText().contains("10"));
        assertTrue(generate.apply("SUMMARIZE").derivedText().contains("máximo"));
        assertEquals("Tabla con 3 filas y 3 columnas.",
                generate.apply("ANNOUNCE_ONLY").derivedText());
        assertTrue(generate.apply("SKIP").derivedText().contains("omitida"));
    }

    @Test
    void explicitRowAndColumnSelectionUsesTheirIntersection() {
        PdfTableStructure table = analyzer.analyze(1, List.of(region("""
                A | B | C
                1 | 2 | 3
                4 | 5 | 6
                """)), Map.of());

        List<PdfTableCell> selected =
                RuleBasedSmallTableNarrationEngine.selectedCells(
                        table, Map.of(
                                "selectedRows", "2-3",
                                "selectedColumns", "2"));

        assertEquals(List.of("2", "5"),
                selected.stream().map(PdfTableCell::text).toList());
        assertThrows(IllegalArgumentException.class, () ->
                RuleBasedSmallTableNarrationEngine.selectedCells(
                        table, Map.of("selectedRows", "8")));
    }

    @Test
    void contextualExplanationRejectsUnknownCellsAndInventedNumbers()
            throws Exception {
        PdfTableStructure table = analyzer.analyze(1, List.of(region("""
                Año | Casos
                2024 | 10
                2025 | 20
                """)), Map.of());
        PdfTableStatistics statistics = analyzer.statistics(table);
        String cited = table.cells().stream()
                .filter(cell -> cell.text().equals("20"))
                .findFirst().orElseThrow().id();
        ContentAnalysisResult valid = new ContentAnalysisResult(
                "El máximo registrado es 20.",
                "{\"summary\":\"El máximo registrado es 20.\","
                        + "\"findings\":[{\"text\":\"Máximo\","
                        + "\"cellIds\":[\"" + cited + "\"]}],"
                        + "\"uncertainties\":[],\"confidence\":0.9}",
                0.9, List.of(), Map.of());
        assertDoesNotThrow(() ->
                TransversalTableExplanationPdfTreatmentEngine
                        .validateGrounding(valid, table, statistics));

        ContentAnalysisResult invented = new ContentAnalysisResult(
                "El máximo registrado es 999.",
                valid.structuredJson().replace("20.", "999."),
                0.9, List.of(), Map.of());
        assertThrows(java.io.IOException.class, () ->
                TransversalTableExplanationPdfTreatmentEngine
                        .validateGrounding(invented, table, statistics));

        ContentAnalysisResult unknownCell = new ContentAnalysisResult(
                "El máximo registrado es 20.",
                valid.structuredJson().replace(
                        cited, "CELL-00000000-0000-0000-0000-000000000000"),
                0.9, List.of(), Map.of());
        assertThrows(java.io.IOException.class, () ->
                TransversalTableExplanationPdfTreatmentEngine
                        .validateGrounding(unknownCell, table, statistics));
    }

    @Test
    void advancedTablePayloadMustBeAnExplicitPpTableResult() {
        assertDoesNotThrow(() ->
                TransversalPpTableStructurePdfTreatmentEngine
                        .validateStructurePayload("""
                                {"schemaVersion":1,"operation":"table",
                                 "results":[{"table_res_list":[]}]}
                                """));
        assertThrows(java.io.IOException.class, () ->
                TransversalPpTableStructurePdfTreatmentEngine
                        .validateStructurePayload("""
                                {"schemaVersion":1,"operation":"layout",
                                 "results":[]}
                                """));
        assertThrows(java.io.IOException.class, () ->
                TransversalPpTableStructurePdfTreatmentEngine
                        .validateStructurePayload(""));
    }

    private static PdfRegion region(String text) {
        return new PdfRegion(
                "TABLE-1", 1, 10, 10, 500, 180,
                0, 0, text, PdfRegionType.TABLE,
                PdfNarratability.NON_NARRATABLE, List.of(),
                new PdfRegionEvidence(
                        PdfRegionOrigin.NATIVE_TEXT, 0.95,
                        "native", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
