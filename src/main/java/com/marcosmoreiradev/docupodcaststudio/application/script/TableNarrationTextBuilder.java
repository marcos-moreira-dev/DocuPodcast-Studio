package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds secondary narration text for source-document tables without promoting them to fragments. */
public final class TableNarrationTextBuilder {
    public String structuredText(DocumentBlock block) {
        if (block == null) {
            return "";
        }
        TableData table = tableData(block.metadata());
        if (table.headers().isEmpty() && table.rows().isEmpty()) {
            return summaryText(block);
        }
        StringBuilder out = new StringBuilder("Lectura de cuadro.");
        if (!table.headers().isEmpty()) {
            out.append(" Encabezados: ").append(String.join("; ", table.headers())).append('.');
        }
        for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            List<String> row = table.rows().get(rowIndex);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            out.append(" Fila ").append(rowIndex + 1).append('.');
            for (int column = 0; column < row.size(); column++) {
                String value = row.get(column);
                if (value.isBlank()) {
                    continue;
                }
                out.append(' ').append(headerFor(table.headers(), column)).append(": ").append(value);
                appendTerminatorIfMissing(out, value);
            }
        }
        return out.toString().strip();
    }

    private static void appendTerminatorIfMissing(StringBuilder output, String value) {
        int cursor = value == null ? -1 : value.length() - 1;
        while (cursor >= 0 && (Character.isWhitespace(value.charAt(cursor))
                || isClosingPunctuation(value.charAt(cursor)))) {
            cursor--;
        }
        if (cursor < 0 || !isSentenceTerminator(value.charAt(cursor))) {
            output.append('.');
        }
    }

    private static boolean isSentenceTerminator(char value) {
        return value == '.' || value == '?' || value == '!' || value == '…';
    }

    private static boolean isClosingPunctuation(char value) {
        return value == '"' || value == '\'' || value == ')' || value == ']'
                || value == '»' || value == '”';
    }

    public String summaryText(DocumentBlock block) {
        if (block == null) {
            return "";
        }
        String rows = firstPresent(block.metadata(), "table.rowCount", "rows");
        String columns = firstPresent(block.metadata(), "table.columnCount", "columns");
        String kind = block.metadata().getOrDefault("table.kind", "").strip();
        if (!rows.isBlank() || !columns.isBlank()) {
            return tableLabel(kind) + " del documento fuente de "
                    + count(rows) + " filas y " + count(columns) + " columnas.";
        }
        String text = block.text() == null ? "" : block.text().strip();
        return text.startsWith("Tabla") ? text : "Tabla detectada: " + text;
    }

    private static TableData tableData(Map<String, String> metadata) {
        List<String> headers = headers(metadata);
        List<List<String>> rows = rows(metadata, headers.size());
        if (headers.isEmpty() && rows.isEmpty()) {
            return markdownData(metadata == null ? "" : metadata.getOrDefault("tableMarkdown", ""));
        }
        return new TableData(headers, rows);
    }

    private static List<String> headers(Map<String, String> metadata) {
        ArrayList<String> values = new ArrayList<>();
        int columns = intValue(firstPresent(metadata, "table.columnCount", "columns"));
        for (int i = 0; i < Math.max(columns, 0); i++) {
            String value = metadata == null ? "" : metadata.getOrDefault("table.header." + i, "").strip();
            if (!value.isBlank()) {
                values.add(value);
            } else if (!values.isEmpty()) {
                values.add("Columna " + (i + 1));
            }
        }
        return List.copyOf(values);
    }

    private static List<List<String>> rows(Map<String, String> metadata, int knownColumns) {
        ArrayList<List<String>> rows = new ArrayList<>();
        int rowCount = intValue(firstPresent(metadata, "table.rowCount", "rows"));
        int columns = Math.max(knownColumns, intValue(firstPresent(metadata, "table.columnCount", "columns")));
        for (int r = 1; r < rowCount; r++) {
            ArrayList<String> row = new ArrayList<>();
            for (int c = 0; c < columns; c++) {
                row.add(metadata == null ? "" : metadata.getOrDefault("table.cell." + r + "." + c, "").strip());
            }
            if (row.stream().anyMatch(value -> !value.isBlank())) {
                rows.add(List.copyOf(row));
            }
        }
        return List.copyOf(rows);
    }

    private static TableData markdownData(String markdown) {
        ArrayList<List<String>> parsed = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return new TableData(List.of(), List.of());
        }
        for (String line : markdown.split("\\R")) {
            String trimmed = line == null ? "" : line.strip();
            if (!trimmed.startsWith("|") || trimmed.matches("[|\\s:.-]+")) {
                continue;
            }
            ArrayList<String> cells = new ArrayList<>();
            for (String part : trimmed.split("\\|")) {
                String cell = part == null ? "" : part.replace("\\|", "|").strip();
                if (!cell.isBlank()) {
                    cells.add(cell);
                }
            }
            if (!cells.isEmpty()) {
                parsed.add(List.copyOf(cells));
            }
        }
        if (parsed.isEmpty()) {
            return new TableData(List.of(), List.of());
        }
        return new TableData(parsed.get(0), parsed.subList(1, parsed.size()));
    }

    private static String headerFor(List<String> headers, int index) {
        if (index >= 0 && index < headers.size() && !headers.get(index).isBlank()) {
            return headers.get(index);
        }
        return "Columna " + (index + 1);
    }

    private static String firstPresent(Map<String, String> metadata, String... keys) {
        if (metadata == null) {
            return "";
        }
        for (String key : keys) {
            String value = metadata.getOrDefault(key, "").strip();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static int intValue(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value.strip());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String count(String value) {
        return value == null || value.isBlank() ? "?" : value.strip();
    }

    private static String tableLabel(String kind) {
        return switch (kind == null ? "" : kind) {
            case "PROSE" -> "Tabla de texto";
            case "KEY_VALUE" -> "Tabla de conceptos y valores";
            case "MIXED" -> "Tabla mixta";
            case "NUMERIC" -> "Tabla numérica";
            case "MATRIX" -> "Matriz matemática";
            case "MATH" -> "Tabla con expresiones matemáticas";
            default -> "Tabla";
        };
    }

    private record TableData(List<String> headers, List<List<String>> rows) {
    }
}
