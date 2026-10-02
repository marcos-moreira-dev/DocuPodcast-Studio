package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import java.util.List;

final class ProjectJsonWriteSupport {
    private ProjectJsonWriteSupport() { }

    static void stringArrayField(StringBuilder out, int level, String name, List<String> values) {
        indent(out, level).append(quote(name)).append(": [");
        for (int i = 0; i < values.size(); i++) {
            out.append(quote(values.get(i)));
            if (i < values.size() - 1) {
                out.append(", ");
            }
        }
        out.append("]");
    }

    static String stringArray(List<String> values) {
        List<String> safeValues = values == null ? List.of() : values;
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < safeValues.size(); i++) {
            out.append(quote(safeValues.get(i)));
            if (i < safeValues.size() - 1) {
                out.append(", ");
            }
        }
        return out.append("]").toString();
    }

    static void field(StringBuilder out, int level, String name, String value) {
        indent(out, level).append(quote(name)).append(": ").append(value);
    }

    static StringBuilder indent(StringBuilder out, int level) {
        return out.append("    ".repeat(level));
    }

    static String quote(String value) {
        String safe = value == null ? "" : value;
        StringBuilder escaped = new StringBuilder(safe.length() + 16);
        escaped.append('"');
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}

