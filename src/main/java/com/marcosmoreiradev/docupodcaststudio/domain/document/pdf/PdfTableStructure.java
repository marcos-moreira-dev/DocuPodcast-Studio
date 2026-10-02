package com.marcosmoreiradev.docupodcaststudio.domain.document.pdf;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Derived, reviewable table structure. It is evidence associated with source
 * regions and never replaces canonical PDF text or geometry.
 */
public record PdfTableStructure(
        String id,
        List<String> sourceRegionIds,
        int rows,
        int columns,
        List<PdfTableCell> cells,
        List<String> headerCellIds,
        String caption,
        List<String> units,
        PdfTableKind kind,
        double confidence,
        Map<String, String> metadata
) {
    public PdfTableStructure {
        id = Objects.toString(id, "").strip();
        if (id.isBlank()) throw new IllegalArgumentException("table id is required");
        sourceRegionIds = sourceRegionIds == null ? List.of()
                : sourceRegionIds.stream().filter(Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        if (sourceRegionIds.isEmpty()) {
            throw new IllegalArgumentException("source regions are required");
        }
        rows = Math.max(0, rows);
        columns = Math.max(0, columns);
        cells = cells == null ? List.of() : List.copyOf(cells);
        headerCellIds = headerCellIds == null ? List.of()
                : headerCellIds.stream().filter(Objects::nonNull)
                .map(String::strip).filter(value -> !value.isBlank())
                .distinct().toList();
        caption = Objects.toString(caption, "").strip();
        units = units == null ? List.of()
                : units.stream().filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().toList();
        kind = Objects.requireNonNullElse(kind, PdfTableKind.UNKNOWN);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (!cells.isEmpty() && (rows == 0 || columns == 0)) {
            throw new IllegalArgumentException(
                    "non-empty table requires positive dimensions");
        }
        for (PdfTableCell cell : cells) {
            if (cell.row() >= rows || cell.column() >= columns
                    || cell.row() + cell.rowSpan() > rows
                    || cell.column() + cell.columnSpan() > columns) {
                throw new IllegalArgumentException(
                        "cell lies outside declared table dimensions");
            }
        }
    }

    public boolean withinFullReadingLimit() {
        return rows <= 12 && columns <= 8;
    }
}
