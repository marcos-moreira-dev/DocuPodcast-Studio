package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic table structure and safe narration baseline. Despite its
 * historical class name it also handles large and numeric tables without
 * reciting them in full.
 */
public class RuleBasedSmallTableNarrationEngine
        implements PdfDerivedTreatmentEngine {
    public static final String ID = "pdf-table-structure-local";
    private final PdfTableStructureAnalyzer analyzer =
            new PdfTableStructureAnalyzer();
    private final PdfDerivedTreatmentKind treatmentKind;

    public RuleBasedSmallTableNarrationEngine() {
        this(PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION);
    }

    protected RuleBasedSmallTableNarrationEngine(
            PdfDerivedTreatmentKind treatmentKind) {
        this.treatmentKind = java.util.Objects.requireNonNull(treatmentKind);
    }

    @Override public String id() { return ID; }
    @Override public String version() { return "2.0"; }
    @Override public PdfDerivedTreatmentKind kind() {
        return treatmentKind;
    }

    @Override
    public PdfDerivedTreatment generate(
            PreparedPdfPage page,
            List<PdfRegion> sourceRegions,
            PdfDerivedTreatmentGenerationRequest request) {
        PdfTableStructure table = analyzer.analyze(
                page.pageNumber(), sourceRegions, request.options());
        PdfTableStatistics statistics = analyzer.statistics(table);
        TableNarrationPolicy narrationPolicy = narrationPolicy(request.options());
        String fingerprint = analyzer.fingerprint(table, statistics);
        String id = "PDF-DER-" + UUID.nameUUIDFromBytes(
                (page.pageNumber() + "|" + fingerprint + "|" + kind())
                        .getBytes(StandardCharsets.UTF_8));
        LinkedHashSet<String> citedCells = new LinkedHashSet<>();
        List<PdfTableCell> selectedCells = selectedCells(
                table, request.options());
        if (!selectedCells.isEmpty()) {
            selectedCells.forEach(cell -> citedCells.add(cell.id()));
        } else {
            statistics.columns().forEach(column -> {
                citedCells.add(column.minimumCellId());
                citedCells.add(column.maximumCellId());
            });
        }
        if (citedCells.isEmpty() && table.withinFullReadingLimit()
                && (table.kind() == PdfTableKind.PROSE
                || table.kind() == PdfTableKind.KEY_VALUE)) {
            table.cells().forEach(cell -> citedCells.add(cell.id()));
        }
        long revision = sourceRegions.stream().mapToLong(PdfRegion::revision)
                .max().orElse(1L);
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("local", "true");
        metadata.put("method", "deterministic-table-structure-v2");
        metadata.put("tableKind", table.kind().name());
        metadata.put("tableStructureJson", analyzer.structureJson(table));
        metadata.put("tableStatisticsJson",
                analyzer.statisticsJson(statistics));
        metadata.put("citedCellIds", String.join(",", citedCells));
        metadata.put("focusCellIds", String.join(",", citedCells));
        table.cells().stream().filter(cell -> citedCells.contains(cell.id()))
                .forEach(cell -> metadata.put("focusCell." + cell.id(),
                        cell.xMin() + "," + cell.yMin() + ","
                                + cell.xMax() + "," + cell.yMax()));
        metadata.put("canonicalTextModified", "false");
        metadata.put("fullReadingAllowed",
                Boolean.toString(table.withinFullReadingLimit()));
        metadata.put("tableNarrationPolicy", narrationPolicy.name());
        return new PdfDerivedTreatment(
                id, kind(), request.sourceRegionIds(),
                narration(table, statistics, selectedCells, narrationPolicy), id(), version(),
                table.confidence(), Instant.now(),
                PdfDerivedTreatmentState.DRAFT, revision, fingerprint,
                "Narración determinista según estructura y estadísticas "
                        + "verificables.",
                metadata);
    }

    private static String narration(PdfTableStructure table,
                                    PdfTableStatistics statistics,
                                    List<PdfTableCell> selectedCells,
                                    TableNarrationPolicy policy) {
        String introduction = "Tabla " + (table.caption().isBlank()
                ? "" : "\"" + table.caption() + "\" ")
                + "con " + table.rows() + " filas y " + table.columns()
                + " columnas. ";
        return switch (policy.canonical()) {
            case SKIP -> "Tabla omitida por la política de lectura.";
            case ANNOUNCE_ONLY -> introduction.strip();
            case READ_ALL -> introduction + readCells(table);
            case READ_TEXTUAL_CONTENT -> introduction
                    + readCells(table.cells().stream()
                    .filter(cell -> PdfTableStructureAnalyzer.number(cell.text()).isEmpty())
                    .toList());
            case SUMMARIZE -> adaptiveNarration(
                    table, statistics, selectedCells, introduction);
            default -> throw new IllegalStateException("Política no normalizada");
        };
    }

    private static String adaptiveNarration(
            PdfTableStructure table, PdfTableStatistics statistics,
            List<PdfTableCell> selectedCells, String introduction) {
        if (!selectedCells.isEmpty()) {
            return introduction + "Selección solicitada: "
                    + readCells(selectedCells);
        }
        return switch (table.kind()) {
            case PROSE, KEY_VALUE -> table.withinFullReadingLimit()
                    ? introduction + readCells(table)
                    : introduction
                    + "La tabla es extensa; selecciona filas o columnas "
                    + "para leerlas.";
            case MIXED -> introduction + headings(table)
                    + summarizeStatistics(statistics)
                    + " La estructura completa queda disponible para revisión.";
            case NUMERIC -> introduction + headings(table)
                    + units(table) + summarizeStatistics(statistics);
            case MATRIX -> introduction
                    + "La región contiene una matriz y debe pasar por el "
                    + "flujo matemático antes de narrarse.";
            case MATH -> introduction
                    + "La tabla contiene fórmulas y debe pasar por el "
                    + "flujo matemático antes de narrarse.";
            case UNKNOWN -> introduction
                    + "No hay contexto suficiente para interpretar su propósito. "
                    + "Se anuncia únicamente la estructura.";
        };
    }

    private static TableNarrationPolicy narrationPolicy(
            Map<String, String> options) {
        String value = options.getOrDefault(
                "narrationPolicy", TableNarrationPolicy.SUMMARIZE.name());
        try {
            return TableNarrationPolicy.valueOf(value.strip()
                    .toUpperCase(java.util.Locale.ROOT)).canonical();
        } catch (IllegalArgumentException invalid) {
            return TableNarrationPolicy.SUMMARIZE;
        }
    }

    private static String readCells(PdfTableStructure table) {
        return readCells(table.cells());
    }

    private static String readCells(List<PdfTableCell> selectedCells) {
        StringBuilder narration = new StringBuilder();
        List<Integer> rows = selectedCells.stream().map(PdfTableCell::row)
                .distinct().sorted().toList();
        for (int row : rows) {
            int currentRow = row;
            List<PdfTableCell> cells = selectedCells.stream()
                    .filter(cell -> cell.row() == currentRow)
                    .sorted(java.util.Comparator.comparingInt(
                            PdfTableCell::column))
                    .toList();
            narration.append("Fila ").append(row + 1).append(": ")
                    .append(cells.stream().map(PdfTableCell::text)
                            .collect(java.util.stream.Collectors.joining("; ")))
                    .append(". ");
        }
        return narration.toString().strip();
    }

    static List<PdfTableCell> selectedCells(
            PdfTableStructure table, Map<String, String> options) {
        java.util.Set<Integer> rows = indexes(
                options.get("selectedRows"), table.rows());
        java.util.Set<Integer> columns = indexes(
                options.get("selectedColumns"), table.columns());
        if (rows.isEmpty() && columns.isEmpty()) return List.of();
        return table.cells().stream()
                .filter(cell -> rows.isEmpty() || rows.contains(cell.row()))
                .filter(cell -> columns.isEmpty()
                        || columns.contains(cell.column()))
                .sorted(java.util.Comparator.comparingInt(PdfTableCell::row)
                        .thenComparingInt(PdfTableCell::column))
                .toList();
    }

    private static java.util.Set<Integer> indexes(
            String specification, int size) {
        if (specification == null || specification.isBlank()) {
            return java.util.Set.of();
        }
        LinkedHashSet<Integer> result = new LinkedHashSet<>();
        for (String token : specification.split(",")) {
            String value = token.strip();
            if (value.isBlank()) continue;
            int separator = value.indexOf('-');
            try {
                int start = Integer.parseInt(
                        separator < 0 ? value : value.substring(0, separator))
                        - 1;
                int end = Integer.parseInt(
                        separator < 0 ? value : value.substring(separator + 1))
                        - 1;
                if (start < 0 || end < start || end >= size) {
                    throw new IllegalArgumentException(
                            "Rango de tabla fuera de límites: " + value);
                }
                for (int index = start; index <= end; index++) {
                    result.add(index);
                }
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException(
                        "Rango de tabla inválido: " + value, invalid);
            }
        }
        return java.util.Set.copyOf(result);
    }

    private static String headings(PdfTableStructure table) {
        List<String> headings = table.cells().stream()
                .filter(cell -> table.headerCellIds().contains(cell.id()))
                .map(PdfTableCell::text)
                .filter(value -> !value.isBlank()).toList();
        return headings.isEmpty() ? ""
                : "Encabezados: " + String.join(", ", headings) + ". ";
    }

    private static String units(PdfTableStructure table) {
        return table.units().isEmpty() ? ""
                : "Unidades visibles: " + String.join(", ", table.units())
                + ". ";
    }

    private static String summarizeStatistics(
            PdfTableStatistics statistics) {
        if (statistics.columns().isEmpty()) {
            return "No se calcularon tendencias numéricas seguras.";
        }
        StringBuilder summary = new StringBuilder();
        for (PdfTableStatistics.ColumnStatistics column
                : statistics.columns()) {
            summary.append("En ").append(column.heading())
                    .append(", el mínimo es ").append(column.minimum())
                    .append(" y el máximo es ").append(column.maximum())
                    .append("; tendencia ").append(switch (column.trend()) {
                        case INCREASING -> "creciente";
                        case DECREASING -> "decreciente";
                        case CONSTANT -> "constante";
                        case NON_MONOTONIC -> "no monótona";
                    }).append(". ");
        }
        return summary.toString().strip();
    }
}
