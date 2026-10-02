package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

final class ProjectJsonReadSupport {
    private ProjectJsonReadSupport() { }

    static Map<String, Object> object(Object value, String name) throws IOException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException(name + " must be an object");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> optionalObject(Object value) throws IOException {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException("Expected object");
        }
        return (Map<String, Object>) map;
    }

    static String string(Object value, String field) throws IOException {
        if (!(value instanceof String string)) {
            throw new IOException(field + " must be a string");
        }
        return string;
    }

    static String stringOrDefault(Object value, String defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String string)) {
            throw new IOException("Expected string value");
        }
        return string;
    }

    static Instant instant(Object value, String field) throws IOException {
        try {
            return Instant.parse(string(value, field));
        } catch (RuntimeException ex) {
            throw new IOException(field + " must be an ISO-8601 instant", ex);
        }
    }

    static int intValue(Object value) throws IOException {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("formatVersion must be a number");
    }

    static int intOrDefault(Object value, int defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IOException("Expected numeric value");
    }

    static long longOrDefault(Object value, long defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IOException("Expected numeric value");
    }

    static double doubleOrDefault(Object value, double defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IOException("Expected numeric value");
    }

    static boolean booleanOrDefault(Object value, boolean defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IOException("Expected boolean value");
    }

    static List<String> stringListOrDefault(Object value, List<String> defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("Expected array of strings");
        }
        ArrayList<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text)) {
                throw new IOException("Expected array of strings");
            }
            result.add(text);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> castList(Object value, String name) throws IOException {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException(name + " must be an array");
        }
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw new IOException(name + " entries must be objects");
            }
            result.add((Map<String, Object>) raw);
        }
        return result;
    }

    static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String field) throws IOException {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new IOException(field + " has unsupported value: " + value, ex);
        }
    }
}

