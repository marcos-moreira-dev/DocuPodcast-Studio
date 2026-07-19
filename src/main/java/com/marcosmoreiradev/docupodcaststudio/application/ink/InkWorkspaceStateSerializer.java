package com.marcosmoreiradev.docupodcaststudio.application.ink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON writer for ink sidecars. Reading legacy sidecars remains in existing adapters. */
public final class InkWorkspaceStateSerializer {
    private InkWorkspaceStateSerializer() {
    }

    @SuppressWarnings("unchecked")
    public static InkWorkspaceState fromJson(String json) throws IOException {
        Object parsed = new Parser(json).parse();
        if (!(parsed instanceof Map<?, ?> rawRoot)) {
            throw new IOException("Ink workspace state must be a JSON object");
        }
        Map<String, Object> root = (Map<String, Object>) rawRoot;
        double width = doubleValue(root.get("width"), 1.0);
        double height = doubleValue(root.get("height"), 1.0);
        String background = stringValue(root.get("background"), "#ffffffff");
        List<InkStroke> strokes = strokes(root.get("inkStrokes"));
        List<InkPlacedImage> images = images(root.get("images"));
        Map<String, String> metadata = metadata(root.get("metadata"));
        return InkWorkspaceState.create(width, height, background, strokes, images, metadata);
    }

    public static String toJson(InkWorkspaceState state) {
        InkWorkspaceState safe = state == null
                ? InkWorkspaceState.create(1, 1, "#ffffffff", List.of(), List.of(), Map.of())
                : state;
        StringBuilder json = new StringBuilder(4096);
        json.append('{');
        json.append("\"version\":").append(InkWorkspaceState.CURRENT_VERSION).append(',');
        field(json, "format", "ink-workspace-state").append(',');
        json.append("\"width\":").append(number(safe.logicalWidth())).append(',');
        json.append("\"height\":").append(number(safe.logicalHeight())).append(',');
        field(json, "background", safe.background()).append(',');
        json.append("\"inkStrokes\":[");
        for (int i = 0; i < safe.strokes().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            stroke(json, safe.strokes().get(i));
        }
        json.append("],\"images\":[");
        for (int i = 0; i < safe.images().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            image(json, safe.images().get(i));
        }
        json.append("],\"metadata\":{");
        int metadataIndex = 0;
        for (Map.Entry<String, String> entry : safe.metadata().entrySet()) {
            if (metadataIndex++ > 0) {
                json.append(',');
            }
            field(json, entry.getKey(), entry.getValue());
        }
        json.append("}}");
        return json.toString();
    }

    private static void stroke(StringBuilder json, InkStroke stroke) {
        InkStroke safe = stroke == null ? InkStroke.draw("#000000ff", 1, List.of()) : stroke;
        json.append('{');
        field(json, "type", safe.tool().name()).append(',');
        field(json, "color", safe.color()).append(',');
        json.append("\"width\":").append(number(safe.width())).append(',');
        json.append("\"points\":[");
        for (int i = 0; i < safe.points().size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            InkPoint point = safe.points().get(i);
            json.append('{');
            json.append("\"x\":").append(number(point.x())).append(',');
            json.append("\"y\":").append(number(point.y())).append(',');
            json.append("\"nanos\":").append(point.nanos()).append(',');
            json.append("\"pressure\":").append(number(point.pressure()));
            json.append('}');
        }
        json.append("]}");
    }

    private static void image(StringBuilder json, InkPlacedImage image) {
        InkPlacedImage safe = image == null
                ? new InkPlacedImage("", "", "", "", "", 0, 0, 1, 1, 0, 0, 1, InkImageCrop.none())
                : image;
        json.append('{');
        field(json, "id", safe.id()).append(',');
        field(json, "sourceAssetId", safe.sourceAssetId()).append(',');
        field(json, "sourcePath", safe.sourcePath()).append(',');
        field(json, "image", safe.inlineImageData()).append(',');
        field(json, "originalImage", safe.inlineOriginalImageData()).append(',');
        json.append("\"x\":").append(number(safe.x())).append(',');
        json.append("\"y\":").append(number(safe.y())).append(',');
        json.append("\"fitWidth\":").append(number(safe.fitWidth())).append(',');
        json.append("\"height\":").append(number(safe.height())).append(',');
        json.append("\"originalLayoutX\":").append(number(safe.originalLayoutX())).append(',');
        json.append("\"originalLayoutY\":").append(number(safe.originalLayoutY())).append(',');
        json.append("\"originalFitWidth\":").append(number(safe.originalFitWidth())).append(',');
        json.append("\"cropActive\":").append(safe.crop().active()).append(',');
        json.append("\"crop\":{");
        json.append("\"active\":").append(safe.crop().active()).append(',');
        json.append("\"originalX\":").append(number(safe.crop().originalX())).append(',');
        json.append("\"originalY\":").append(number(safe.crop().originalY())).append(',');
        json.append("\"originalWidth\":").append(number(safe.crop().originalWidth())).append(',');
        json.append("\"originalHeight\":").append(number(safe.crop().originalHeight()));
        json.append("}}");
    }

    private static StringBuilder field(StringBuilder json, String name, String value) {
        quote(json, name == null ? "" : name).append(':');
        quote(json, value == null ? "" : value);
        return json;
    }

    private static StringBuilder quote(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> json.append("\\\\");
                case '"' -> json.append("\\\"");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> json.append(c);
            }
        }
        json.append('"');
        return json;
    }

    private static String number(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    @SuppressWarnings("unchecked")
    private static List<InkStroke> strokes(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        ArrayList<InkStroke> strokes = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawStroke)) {
                continue;
            }
            Map<String, Object> stroke = (Map<String, Object>) rawStroke;
            InkTool tool = InkTool.fromToken(stringValue(stroke.get("type"), "DRAW"));
            String color = stringValue(stroke.get("color"), "#000000ff");
            double width = doubleValue(stroke.get("width"), 1.0);
            ArrayList<InkPoint> points = new ArrayList<>();
            if (stroke.get("points") instanceof List<?> rawPoints) {
                for (Object pointItem : rawPoints) {
                    if (!(pointItem instanceof Map<?, ?> rawPoint)) {
                        continue;
                    }
                    Map<String, Object> point = (Map<String, Object>) rawPoint;
                    points.add(InkPoint.of(
                            doubleValue(point.get("x"), 0.0),
                            doubleValue(point.get("y"), 0.0),
                            longValue(point.get("nanos"), 0L),
                            doubleValue(point.get("pressure"), 1.0)));
                }
            }
            strokes.add(new InkStroke(tool, color, width, points));
        }
        return List.copyOf(strokes);
    }

    @SuppressWarnings("unchecked")
    private static List<InkPlacedImage> images(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        ArrayList<InkPlacedImage> images = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawImage)) {
                continue;
            }
            Map<String, Object> image = (Map<String, Object>) rawImage;
            Map<String, Object> crop = image.get("crop") instanceof Map<?, ?> rawCrop
                    ? (Map<String, Object>) rawCrop
                    : Map.of();
            boolean cropActive = booleanValue(crop.get("active"),
                    booleanValue(image.get("cropActive"), false));
            images.add(new InkPlacedImage(
                    stringValue(image.get("id"), ""),
                    stringValue(image.get("sourceAssetId"), ""),
                    stringValue(image.get("sourcePath"), ""),
                    stringValue(image.get("image"), ""),
                    stringValue(image.get("originalImage"), ""),
                    doubleValue(image.get("x"), 0.0),
                    doubleValue(image.get("y"), 0.0),
                    doubleValue(image.get("fitWidth"), 1.0),
                    doubleValue(image.get("height"), 1.0),
                    doubleValue(image.get("originalLayoutX"), 0.0),
                    doubleValue(image.get("originalLayoutY"), 0.0),
                    doubleValue(image.get("originalFitWidth"), 1.0),
                    new InkImageCrop(
                            cropActive,
                            doubleValue(crop.get("originalX"), 0.0),
                            doubleValue(crop.get("originalY"), 0.0),
                            doubleValue(crop.get("originalWidth"), 0.0),
                            doubleValue(crop.get("originalHeight"), 0.0))));
        }
        return List.copyOf(images);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> metadata(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        ((Map<String, Object>) rawMap).forEach((key, item) -> metadata.put(key, stringValue(item, "")));
        return Map.copyOf(metadata);
    }

    private static String stringValue(Object value, String fallback) {
        return value instanceof String text ? text : fallback;
    }

    private static double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parse() throws IOException {
            Object value = readValue();
            skipWhitespace();
            if (!end()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object readValue() throws IOException {
            skipWhitespace();
            if (end()) {
                throw error("Unexpected end of JSON");
            }
            return switch (peek()) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() throws IOException {
            expect('{');
            LinkedHashMap<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            do {
                String key = readString();
                skipWhitespace();
                expect(':');
                object.put(key, readValue());
                skipWhitespace();
            } while (consume(','));
            expect('}');
            return object;
        }

        private List<Object> readArray() throws IOException {
            expect('[');
            ArrayList<Object> values = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return values;
            }
            do {
                values.add(readValue());
                skipWhitespace();
            } while (consume(','));
            expect(']');
            return values;
        }

        private String readString() throws IOException {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!end()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return builder.toString();
                }
                if (c == '\\') {
                    if (end()) {
                        throw error("Incomplete escape sequence");
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(readUnicode());
                        default -> throw error("Unsupported escape sequence: \\" + escaped);
                    }
                } else {
                    builder.append(c);
                }
            }
            throw error("Unterminated string");
        }

        private char readUnicode() throws IOException {
            if (index + 4 > text.length()) {
                throw error("Incomplete unicode escape");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw error("Invalid unicode escape");
            }
        }

        private Object readLiteral(String literal, Object value) throws IOException {
            if (!text.startsWith(literal, index)) {
                throw error("Expected " + literal);
            }
            index += literal.length();
            return value;
        }

        private Number readNumber() throws IOException {
            int start = index;
            if (peek() == '-') {
                index++;
            }
            while (!end() && Character.isDigit(peek())) {
                index++;
            }
            boolean floating = false;
            if (!end() && peek() == '.') {
                floating = true;
                index++;
                while (!end() && Character.isDigit(peek())) {
                    index++;
                }
            }
            if (!end() && (peek() == 'e' || peek() == 'E')) {
                floating = true;
                index++;
                if (!end() && (peek() == '+' || peek() == '-')) {
                    index++;
                }
                while (!end() && Character.isDigit(peek())) {
                    index++;
                }
            }
            if (start == index) {
                throw error("Expected JSON value");
            }
            String token = text.substring(start, index);
            try {
                return floating ? Double.parseDouble(token) : Long.parseLong(token);
            } catch (NumberFormatException ex) {
                throw error("Invalid number: " + token);
            }
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (end() || text.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean consume(char expected) {
            skipWhitespace();
            if (!end() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            return text.charAt(index);
        }

        private void skipWhitespace() {
            while (!end() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private boolean end() {
            return index >= text.length();
        }

        private IOException error(String message) {
            return new IOException(message + " at character " + index);
        }
    }
}
