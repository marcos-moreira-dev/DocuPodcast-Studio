package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/** Small JSON helpers kept inside the adapter to avoid leaking a provider dependency into the API. */
final class OllamaJson {
    private OllamaJson() { }

    /**
     * Returns the first complete JSON object embedded in a model response.
     * Structured-output providers normally return the object directly, but
     * local models can still wrap it in a Markdown fence or a short preamble.
     */
    static String firstObject(String value) {
        if (value == null || value.isBlank()) return "";
        int start = value.indexOf('{');
        if (start < 0) return "";
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = start; index < value.length(); index++) {
            char character = value.charAt(index);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    quoted = false;
                }
                continue;
            }
            if (character == '"') {
                quoted = true;
            } else if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return value.substring(start, index + 1);
            }
        }
        return "";
    }

    static String quote(String value) {
        String safe = value == null ? "" : value;
        StringBuilder out = new StringBuilder(safe.length() + 16).append('"');
        for (int index = 0; index < safe.length(); index++) {
            char character = safe.charAt(index);
            switch (character) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (character < 0x20) out.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    else out.append(character);
                }
            }
        }
        return out.append('"').toString();
    }

    static String stringProperty(String json, String property) {
        if (json == null || property == null) return "";
        int key = json.indexOf(quote(property));
        if (key < 0) return "";
        int colon = json.indexOf(':', key + property.length() + 2);
        if (colon < 0) return "";
        int start = skipWhitespace(json, colon + 1);
        if (start >= json.length() || json.charAt(start) != '"') return "";
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int index = start + 1; index < json.length(); index++) {
            char character = json.charAt(index);
            if (escaped) {
                switch (character) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '"', '\\', '/' -> out.append(character);
                    case 'u' -> {
                        if (index + 4 < json.length()) {
                            out.append((char) Integer.parseInt(json.substring(index + 1, index + 5), 16));
                            index += 4;
                        }
                    }
                    default -> out.append(character);
                }
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                return out.toString();
            } else {
                out.append(character);
            }
        }
        return "";
    }

    static double numberProperty(String json, String property, double fallback) {
        if (json == null || property == null) return fallback;
        int key = json.indexOf(quote(property));
        if (key < 0) return fallback;
        int colon = json.indexOf(':', key + property.length() + 2);
        if (colon < 0) return fallback;
        int start = skipWhitespace(json, colon + 1);
        int end = start;
        while (end < json.length() && "-+.0123456789eE".indexOf(json.charAt(end)) >= 0) end++;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    static List<String> stringArrayProperty(String json, String property) {
        if (json == null || property == null) return List.of();
        int key = json.indexOf(quote(property));
        if (key < 0) return List.of();
        int colon = json.indexOf(':', key + property.length() + 2);
        if (colon < 0) return List.of();
        int cursor = skipWhitespace(json, colon + 1);
        if (cursor >= json.length() || json.charAt(cursor) != '[') return List.of();
        ArrayList<String> result = new ArrayList<>();
        cursor++;
        while (cursor < json.length()) {
            cursor = skipWhitespace(json, cursor);
            if (cursor >= json.length() || json.charAt(cursor) == ']') break;
            if (json.charAt(cursor) != '"') return List.of();
            String tail = json.substring(cursor);
            String value = stringProperty("{\"value\":" + tail, "value");
            result.add(value);
            cursor++;
            boolean escaped = false;
            while (cursor < json.length()) {
                char character = json.charAt(cursor++);
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') break;
            }
            cursor = skipWhitespace(json, cursor);
            if (cursor < json.length() && json.charAt(cursor) == ',') cursor++;
        }
        return List.copyOf(result);
    }

    private static int skipWhitespace(String value, int index) {
        int current = index;
        while (current < value.length() && Character.isWhitespace(value.charAt(current))) current++;
        return current;
    }
}
