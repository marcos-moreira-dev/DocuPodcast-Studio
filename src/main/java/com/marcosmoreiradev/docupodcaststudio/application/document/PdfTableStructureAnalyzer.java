package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic baseline shared by review UI, narration and Qwen grounding. */
public final class PdfTableStructureAnalyzer {
    private static final Pattern UNIT = Pattern.compile(
            "(?i)(%|°[CF]|kg|g|mg|km|m|cm|mm|l|ml|s|min|h|hz|khz|mhz|"
                    + "pa|kpa|mpa|v|mv|a|ma|w|kw|j|kj|mol|usd|eur|€|\\$)");
    private static final Pattern MATH = Pattern.compile(
            ".*(?:[=∑∫√±×÷]|\\b(?:sin|cos|tan|log|ln)\\s*\\().*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public PdfTableStructure analyze(int pageNumber,
                                     List<PdfRegion> regions,
                                     Map<String, String> options) {
        List<PdfRegion> sources = regions == null ? List.of() : regions.stream()
                .sorted(Comparator.comparingInt(PdfRegion::effectiveReadingOrder))
                .toList();
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("La tabla no contiene regiones fuente.");
        }
        Map<String, String> safeOptions = options == null ? Map.of() : options;
        String editedTableText = safeOptions.getOrDefault(
                "editedTableText", "").strip();
        List<List<String>> rows = editedTableText.isBlank()
                ? parseRows(sources) : parseText(editedTableText);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "No se pudo separar el texto de la tabla en celdas.");
        }
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns < 2) {
            throw new IllegalArgumentException(
                    "La estructura detectada tiene menos de dos columnas.");
        }
        String tableId = "PDF-TABLE-" + UUID.nameUUIDFromBytes(
                (pageNumber + "|" + sources.stream().map(PdfRegion::id)
                        .reduce("", (left, right) -> left + "|" + right))
                        .getBytes(StandardCharsets.UTF_8));
        double xMin = sources.stream().mapToDouble(PdfRegion::xMin).min().orElse(0);
        double yMin = sources.stream().mapToDouble(PdfRegion::yMin).min().orElse(0);
        double xMax = sources.stream().mapToDouble(PdfRegion::xMax).max().orElse(xMin);
        double yMax = sources.stream().mapToDouble(PdfRegion::yMax).max().orElse(yMin);
        double cellWidth = (xMax - xMin) / columns;
        double cellHeight = (yMax - yMin) / rows.size();
        double confidence = sources.stream()
                .mapToDouble(region -> region.evidence().confidence())
                .average().orElse(0.5);
        Map<CellCoordinate, CellGeometry> observed = geometries(
                safeOptions.get("observedCellGeometry"),
                PdfTableGeometryEvidence.OBSERVED);
        Map<CellCoordinate, CellGeometry> manual = geometries(
                safeOptions.get("manualCellGeometry"),
                PdfTableGeometryEvidence.MANUAL);
        ArrayList<PdfTableCell> cells = new ArrayList<>();
        for (int row = 0; row < rows.size(); row++) {
            List<String> values = rows.get(row);
            for (int column = 0; column < columns; column++) {
                String text = column < values.size() ? values.get(column) : "";
                String cellId = "CELL-" + UUID.nameUUIDFromBytes(
                        (tableId + "|" + row + "|" + column)
                                .getBytes(StandardCharsets.UTF_8));
                CellCoordinate coordinate = new CellCoordinate(row, column);
                CellGeometry geometry = manual.getOrDefault(coordinate,
                        observed.get(coordinate));
                if (coveredBySpan(row, column, manual, observed)) continue;
                cells.add(new PdfTableCell(
                        cellId, row, column,
                        geometry == null ? 1 : geometry.rowSpan(),
                        geometry == null ? 1 : geometry.columnSpan(),
                        geometry == null ? xMin + column * cellWidth : geometry.xMin(),
                        geometry == null ? yMin + row * cellHeight : geometry.yMin(),
                        geometry == null ? xMin + (column + 1) * cellWidth : geometry.xMax(),
                        geometry == null ? yMin + (row + 1) * cellHeight : geometry.yMax(),
                        text, confidence, geometry == null
                        ? PdfTableGeometryEvidence.INFERRED : geometry.evidence()));
            }
        }
        boolean header = hasHeader(rows);
        List<String> headers = header
                ? cells.stream().filter(cell -> cell.row() == 0)
                .map(PdfTableCell::id).toList()
                : List.of();
        List<String> units = cells.stream()
                .flatMap(cell -> units(cell.text()).stream()).distinct().toList();
        return new PdfTableStructure(
                tableId, sources.stream().map(PdfRegion::id).toList(),
                rows.size(), columns, cells, headers,
                safeOptions.getOrDefault("caption", "").strip(), units,
                classify(rows, header, safeOptions), confidence, Map.of(
                "method", "deterministic-text-grid",
                "geometryEvidence", manual.isEmpty()
                        ? (observed.isEmpty() ? "INFERRED" : "OBSERVED")
                        : "MANUAL",
                "canonicalTextModified", "false",
                "manualCellEdit", Boolean.toString(!editedTableText.isBlank()),
                "manualGeometryEdit", Boolean.toString(!manual.isEmpty())));
    }

    public PdfTableStatistics statistics(PdfTableStructure table) {
        LinkedHashMap<String, Double> values = new LinkedHashMap<>();
        table.cells().forEach(cell ->
                number(cell.text()).ifPresent(value -> values.put(cell.id(), value)));
        ArrayList<PdfTableStatistics.ColumnStatistics> columns = new ArrayList<>();
        for (int column = 0; column < table.columns(); column++) {
            int selectedColumn = column;
            List<PdfTableCell> candidates = table.cells().stream()
                    .filter(cell -> cell.column() == selectedColumn)
                    .filter(cell -> !table.headerCellIds().contains(cell.id()))
                    .filter(cell -> values.containsKey(cell.id())).toList();
            if (candidates.isEmpty()) continue;
            PdfTableCell minimum = candidates.stream()
                    .min(Comparator.comparingDouble(cell -> values.get(cell.id())))
                    .orElseThrow();
            PdfTableCell maximum = candidates.stream()
                    .max(Comparator.comparingDouble(cell -> values.get(cell.id())))
                    .orElseThrow();
            List<Double> ordered = candidates.stream()
                    .sorted(Comparator.comparingInt(PdfTableCell::row))
                    .map(cell -> values.get(cell.id())).toList();
            String heading = table.cells().stream()
                    .filter(cell -> cell.column() == selectedColumn)
                    .filter(cell -> table.headerCellIds().contains(cell.id()))
                    .map(PdfTableCell::text).findFirst()
                    .orElse("Columna " + (column + 1));
            columns.add(new PdfTableStatistics.ColumnStatistics(
                    column, heading, values.get(minimum.id()), minimum.id(),
                    values.get(maximum.id()), maximum.id(),
                    ordered.getLast() - ordered.getFirst(),
                    trend(ordered), ordered.size()));
        }
        return new PdfTableStatistics(columns, values);
    }

    public String structureJson(PdfTableStructure table) {
        StringBuilder json = new StringBuilder("{\"tableId\":")
                .append(quote(table.id()))
                .append(",\"kind\":").append(quote(table.kind().name()))
                .append(",\"rows\":").append(table.rows())
                .append(",\"columns\":").append(table.columns())
                .append(",\"caption\":").append(quote(table.caption()))
                .append(",\"units\":").append(strings(table.units()))
                .append(",\"headerCellIds\":").append(strings(table.headerCellIds()))
                .append(",\"cells\":[");
        for (int index = 0; index < table.cells().size(); index++) {
            PdfTableCell cell = table.cells().get(index);
            if (index > 0) json.append(',');
            json.append("{\"id\":").append(quote(cell.id()))
                    .append(",\"row\":").append(cell.row())
                    .append(",\"column\":").append(cell.column())
                    .append(",\"rowSpan\":").append(cell.rowSpan())
                    .append(",\"columnSpan\":").append(cell.columnSpan())
                    .append(",\"xMin\":").append(cell.xMin())
                    .append(",\"yMin\":").append(cell.yMin())
                    .append(",\"xMax\":").append(cell.xMax())
                    .append(",\"yMax\":").append(cell.yMax())
                    .append(",\"text\":").append(quote(cell.text()))
                    .append(",\"confidence\":").append(cell.confidence())
                    .append(",\"geometryEvidence\":")
                    .append(quote(cell.geometryEvidence().name()))
                    .append('}');
        }
        return json.append("]}").toString();
    }

    public String statisticsJson(PdfTableStatistics statistics) {
        StringBuilder json = new StringBuilder("{\"columns\":[");
        for (int index = 0; index < statistics.columns().size(); index++) {
            PdfTableStatistics.ColumnStatistics column =
                    statistics.columns().get(index);
            if (index > 0) json.append(',');
            json.append("{\"column\":").append(column.column())
                    .append(",\"heading\":").append(quote(column.heading()))
                    .append(",\"minimum\":").append(column.minimum())
                    .append(",\"minimumCellId\":")
                    .append(quote(column.minimumCellId()))
                    .append(",\"maximum\":").append(column.maximum())
                    .append(",\"maximumCellId\":")
                    .append(quote(column.maximumCellId()))
                    .append(",\"variation\":").append(column.variation())
                    .append(",\"trend\":").append(quote(column.trend().name()))
                    .append(",\"values\":").append(column.values())
                    .append('}');
        }
        return json.append("]}").toString();
    }

    public String fingerprint(PdfTableStructure table,
                              PdfTableStatistics statistics) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(structureJson(table).getBytes(StandardCharsets.UTF_8));
            digest.update(statisticsJson(statistics).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static List<List<String>> parseRows(List<PdfRegion> regions) {
        ArrayList<List<String>> rows = new ArrayList<>();
        for (PdfRegion region : regions) {
            rows.addAll(parseText(region.effectiveText()));
        }
        return List.copyOf(rows);
    }

    /**
     * Parses one-based row/column geometry entries:
     * row,column,rowSpan,columnSpan,xMin,yMin,xMax,yMax;...
     */
    private static Map<CellCoordinate, CellGeometry> geometries(
            String specification, PdfTableGeometryEvidence evidence) {
        if (specification == null || specification.isBlank()) return Map.of();
        LinkedHashMap<CellCoordinate, CellGeometry> result = new LinkedHashMap<>();
        for (String entry : specification.split(";")) {
            if (entry.isBlank()) continue;
            String[] values = entry.strip().split("\\s*,\\s*");
            if (values.length != 8) {
                throw new IllegalArgumentException(
                        "Geometría de celda inválida: " + entry);
            }
            try {
                int row = Integer.parseInt(values[0]) - 1;
                int column = Integer.parseInt(values[1]) - 1;
                CellGeometry geometry = new CellGeometry(
                        Math.max(1, Integer.parseInt(values[2])),
                        Math.max(1, Integer.parseInt(values[3])),
                        Double.parseDouble(values[4]),
                        Double.parseDouble(values[5]),
                        Double.parseDouble(values[6]),
                        Double.parseDouble(values[7]), evidence);
                if (row < 0 || column < 0 || geometry.xMax() < geometry.xMin()
                        || geometry.yMax() < geometry.yMin()) {
                    throw new IllegalArgumentException();
                }
                result.put(new CellCoordinate(row, column), geometry);
            } catch (RuntimeException invalid) {
                throw new IllegalArgumentException(
                        "Geometría de celda inválida: " + entry, invalid);
            }
        }
        return Map.copyOf(result);
    }

    private static boolean coveredBySpan(
            int row, int column, Map<CellCoordinate, CellGeometry> manual,
            Map<CellCoordinate, CellGeometry> observed) {
        return java.util.stream.Stream.concat(manual.entrySet().stream(),
                        observed.entrySet().stream())
                .anyMatch(entry -> !(entry.getKey().row() == row
                        && entry.getKey().column() == column)
                        && row >= entry.getKey().row()
                        && row < entry.getKey().row() + entry.getValue().rowSpan()
                        && column >= entry.getKey().column()
                        && column < entry.getKey().column()
                        + entry.getValue().columnSpan());
    }

    private record CellCoordinate(int row, int column) { }

    private record CellGeometry(int rowSpan, int columnSpan,
                                double xMin, double yMin,
                                double xMax, double yMax,
                                PdfTableGeometryEvidence evidence) { }

    private static List<List<String>> parseText(String text) {
        ArrayList<List<String>> rows = new ArrayList<>();
        for (String line : (text == null ? "" : text).split("\\R")) {
            String normalized = line.strip();
            if (normalized.isBlank()) continue;
            String[] parts = normalized.contains("|")
                    ? normalized.split("\\s*\\|\\s*", -1)
                    : normalized.contains("\t")
                    ? normalized.split("\\t+", -1)
                    : normalized.split("\\s{2,}", -1);
            List<String> cells = java.util.Arrays.stream(parts)
                    .map(String::strip).toList();
            if (cells.size() > 1) rows.add(cells);
        }
        return List.copyOf(rows);
    }

    private static boolean hasHeader(List<List<String>> rows) {
        if (rows.size() < 2) return false;
        double firstNumeric = numericRatio(rows.getFirst());
        double remainingNumeric = rows.stream().skip(1)
                .mapToDouble(PdfTableStructureAnalyzer::numericRatio)
                .average().orElse(0.0);
        return firstNumeric < 0.35 && remainingNumeric > firstNumeric + 0.15;
    }

    private static PdfTableKind classify(
            List<List<String>> rows, boolean header, Map<String, String> options) {
        String forced = options.getOrDefault("tableKind", "");
        if (!forced.isBlank()) {
            try {
                return PdfTableKind.valueOf(
                        forced.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Continue with deterministic evidence.
            }
        }
        String all = rows.stream().flatMap(List::stream)
                .reduce("", (left, right) -> left + " " + right);
        long formulaCells = rows.stream().flatMap(List::stream)
                .filter(value -> MATH.matcher(value).matches()).count();
        if (MATH.matcher(all).matches()
                && formulaCells >= Math.max(1, rows.size() / 2)) {
            return PdfTableKind.MATH;
        }
        double numeric = rows.stream().skip(header ? 1 : 0)
                .mapToDouble(PdfTableStructureAnalyzer::numericRatio)
                .average().orElse(0.0);
        int columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (!header && numeric > 0.9 && rows.size() >= 2 && columns >= 2
                && rows.size() <= 12 && columns <= 12) {
            return PdfTableKind.MATRIX;
        }
        if (columns == 2 && numeric < 0.55
                && rows.stream().skip(header ? 1 : 0)
                .filter(row -> !row.isEmpty() && number(row.getFirst()).isEmpty())
                .count() >= Math.max(1, rows.size() - (header ? 1 : 0) - 1)) {
            return PdfTableKind.KEY_VALUE;
        }
        if (numeric >= 0.6) return PdfTableKind.NUMERIC;
        if (numeric <= 0.12) return PdfTableKind.PROSE;
        return PdfTableKind.MIXED;
    }

    private static double numericRatio(List<String> row) {
        if (row == null || row.isEmpty()) return 0.0;
        return row.stream().filter(value -> number(value).isPresent()).count()
                / (double) row.size();
    }

    static java.util.Optional<Double> number(String text) {
        String value = text == null ? "" : text.strip()
                .replace('\u2212', '-')
                .replaceAll("(?i)(?<=\\d)\\s*(?:%|°[CF]|kg|g|mg|km|m|cm|mm|"
                        + "l|ml|s|min|h|hz|khz|mhz|pa|kpa|mpa|v|mv|a|ma|"
                        + "w|kw|j|kj|mol|usd|eur)$", "")
                .replaceAll("^[€$]\\s*", "")
                .replace(" ", "");
        if (value.matches("[-+]?\\d{1,3}(?:\\.\\d{3})+,\\d+(?:[eE][-+]?\\d+)?")) {
            value = value.replace(".", "").replace(',', '.');
        } else if (value.matches("[-+]?\\d+,\\d+(?:[eE][-+]?\\d+)?")) {
            value = value.replace(',', '.');
        } else {
            value = value.replace(",", "");
        }
        if (!value.matches("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"
                + "(?:[eE][-+]?\\d+)?")) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException invalid) {
            return java.util.Optional.empty();
        }
    }

    private static List<String> units(String text) {
        ArrayList<String> units = new ArrayList<>();
        Matcher matcher = UNIT.matcher(text == null ? "" : text);
        while (matcher.find()) units.add(matcher.group());
        return units;
    }

    private static PdfTableStatistics.ColumnStatistics.Trend trend(
            List<Double> values) {
        if (values.size() < 2) {
            return PdfTableStatistics.ColumnStatistics.Trend.CONSTANT;
        }
        boolean increasing = true;
        boolean decreasing = true;
        boolean constant = true;
        for (int index = 1; index < values.size(); index++) {
            int comparison = Double.compare(values.get(index), values.get(index - 1));
            if (comparison < 0) increasing = false;
            if (comparison > 0) decreasing = false;
            if (comparison != 0) constant = false;
        }
        if (constant) return PdfTableStatistics.ColumnStatistics.Trend.CONSTANT;
        if (increasing) return PdfTableStatistics.ColumnStatistics.Trend.INCREASING;
        if (decreasing) return PdfTableStatistics.ColumnStatistics.Trend.DECREASING;
        return PdfTableStatistics.ColumnStatistics.Trend.NON_MONOTONIC;
    }

    private static String strings(List<String> values) {
        return values.stream().map(PdfTableStructureAnalyzer::quote)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\r", "\\r")
                .replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
