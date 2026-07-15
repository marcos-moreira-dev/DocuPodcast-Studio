package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON parser for DocuPodcast project files. */
public final class SimpleJsonParser {
    private final String text;
    private int index;

    private SimpleJsonParser(String text) {
        this.text = text == null ? "" : text;
    }

    public static Object parse(String text) throws IOException {
        SimpleJsonParser parser = new SimpleJsonParser(text);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.end()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    private Object readValue() throws IOException {
        skipWhitespace();
        if (end()) {
            throw error("Unexpected end of JSON");
        }
        char c = peek();
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> {
                if (c == '-' || Character.isDigit(c)) {
                    yield readNumber();
                }
                throw error("Unexpected character '" + c + "'");
            }
        };
    }

    private Map<String, Object> readObject() throws IOException {
        expect('{');
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (consume('}')) {
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            Object value = readValue();
            map.put(key, value);
            skipWhitespace();
            if (consume('}')) {
                return map;
            }
            expect(',');
        }
    }

    private List<Object> readArray() throws IOException {
        expect('[');
        ArrayList<Object> list = new ArrayList<>();
        skipWhitespace();
        if (consume(']')) {
            return list;
        }
        while (true) {
            list.add(readValue());
            skipWhitespace();
            if (consume(']')) {
                return list;
            }
            expect(',');
        }
    }

    private String readString() throws IOException {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (!end()) {
            char c = next();
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                if (end()) {
                    throw error("Unterminated escape sequence");
                }
                char escaped = next();
                switch (escaped) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> out.append(readUnicodeEscape());
                    default -> throw error("Unsupported escape sequence \\" + escaped + "'");
                }
            } else {
                out.append(c);
            }
        }
        throw error("Unterminated string");
    }

    private char readUnicodeEscape() throws IOException {
        if (index + 4 > text.length()) {
            throw error("Invalid unicode escape");
        }
        String hex = text.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException ex) {
            throw error("Invalid unicode escape: " + hex);
        }
    }

    private Number readNumber() {
        int start = index;
        if (peek() == '-') {
            index++;
        }
        while (!end() && Character.isDigit(peek())) {
            index++;
        }
        if (!end() && peek() == '.') {
            index++;
            while (!end() && Character.isDigit(peek())) {
                index++;
            }
            return Double.parseDouble(text.substring(start, index));
        }
        return Long.parseLong(text.substring(start, index));
    }

    private Object readLiteral(String literal, Object value) throws IOException {
        if (!text.startsWith(literal, index)) {
            throw error("Expected literal " + literal);
        }
        index += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (!end() && Character.isWhitespace(peek())) {
            index++;
        }
    }

    private boolean consume(char expected) {
        if (!end() && peek() == expected) {
            index++;
            return true;
        }
        return false;
    }

    private void expect(char expected) throws IOException {
        if (end() || peek() != expected) {
            throw error("Expected '" + expected + "'");
        }
        index++;
    }

    private char peek() {
        return text.charAt(index);
    }

    private char next() {
        return text.charAt(index++);
    }

    private boolean end() {
        return index >= text.length();
    }

    private IOException error(String message) {
        return new IOException(message + " at character " + index);
    }
}
