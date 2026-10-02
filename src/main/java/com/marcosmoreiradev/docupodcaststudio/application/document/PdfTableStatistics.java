package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;
import java.util.Map;

/** Deterministic statistics calculated from parsed cells, never by a model. */
public record PdfTableStatistics(
        List<ColumnStatistics> columns,
        Map<String, Double> numericCellValues
) {
    public PdfTableStatistics {
        columns = columns == null ? List.of() : List.copyOf(columns);
        numericCellValues = numericCellValues == null
                ? Map.of() : Map.copyOf(numericCellValues);
    }

    public record ColumnStatistics(
            int column,
            String heading,
            double minimum,
            String minimumCellId,
            double maximum,
            String maximumCellId,
            double variation,
            Trend trend,
            int values
    ) {
        public enum Trend {
            INCREASING,
            DECREASING,
            CONSTANT,
            NON_MONOTONIC
        }
    }
}
