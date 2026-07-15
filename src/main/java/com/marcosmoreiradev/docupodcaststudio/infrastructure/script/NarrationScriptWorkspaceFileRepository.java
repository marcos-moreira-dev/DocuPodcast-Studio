package com.marcosmoreiradev.docupodcaststudio.infrastructure.script;

import com.marcosmoreiradev.docupodcaststudio.application.script.MaterializedNarrationScript;
import com.marcosmoreiradev.docupodcaststudio.application.script.NarrationScriptWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

/** File-system writer for the internal prepared-reading snapshot used by the .docupodcast project folder. */
public final class NarrationScriptWorkspaceFileRepository implements NarrationScriptWorkspaceRepository {
    @Override
    public MaterializedNarrationScript materialize(NarrationScriptDocument script, Path projectFile) throws IOException {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(projectFile, "projectFile");
        Path root = projectFile.toAbsolutePath().getParent();
        if (root == null) {
            root = Path.of(".").toAbsolutePath();
        }
        Path scriptDir = root.resolve("script");
        Files.createDirectories(scriptDir);
        Path scriptJson = scriptDir.resolve("narration-script.json");
        Files.writeString(scriptJson, toJson(script), StandardCharsets.UTF_8);
        ProjectAssetReference asset = new ProjectAssetReference(
                "SCRIPT-001",
                ProjectAssetKind.NARRATION_SCRIPT,
                "Lectura preparada",
                "script/narration-script.json",
                "application/json",
                "Lectura preparada interna generada desde el documento importado",
                "",
                "Generado por DocuPodcast Studio"
        );
        return new MaterializedNarrationScript(asset);
    }


    @Override
    public Optional<NarrationScriptDocument> load(Path projectFile) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        if (root == null) {
            root = Path.of(".").toAbsolutePath().normalize();
        }
        Path scriptJson = root.resolve("script").resolve("narration-script.json").normalize();
        if (!Files.isRegularFile(scriptJson)) {
            return Optional.empty();
        }
        Object parsed = SimpleJsonParser.parse(Files.readString(scriptJson, StandardCharsets.UTF_8));
        Map<String, Object> map = object(parsed, "script/narration-script.json");
        return Optional.of(new NarrationScriptDocument(
                stringOrDefault(map.get("id"), "SCRIPT-001"),
                stringOrDefault(map.get("title"), "Lectura preparada"),
                stringOrDefault(map.get("language"), "es"),
                stringOrDefault(map.get("sourceDocumentTitle"), ""),
                readSegments(map.get("segments")),
                instantOrNow(map.get("createdAt")),
                instantOrNow(map.get("updatedAt")),
                stringOrDefault(map.get("notes"), "")
        ));
    }

    private static List<NarrationSegment> readSegments(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("script.segments must be an array");
        }
        ArrayList<NarrationSegment> segments = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> segment = object(item, "script.segments[]");
            segments.add(new NarrationSegment(
                    string(segment.get("id"), "segment.id"),
                    enumOrDefault(NarrationSegmentType.class, stringOrDefault(segment.get("type"), NarrationSegmentType.PARAGRAPH.name()), NarrationSegmentType.PARAGRAPH),
                    stringOrDefault(segment.get("title"), ""),
                    stringOrDefault(segment.get("narrationText"), ""),
                    readStringList(segment.get("sourceBlockIds")),
                    stringOrDefault(segment.get("characterId"), "CHR-NARRATOR"),
                    stringOrDefault(segment.get("voiceProfileId"), "VOC-NARRATOR"),
                    stringOrDefault(segment.get("performanceStyleId"), "STY-NEUTRAL"),
                    readStringMap(optionalObject(segment.get("metadata")))
            ));
        }
        return List.copyOf(segments);
    }

    private static List<String> readStringList(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("Expected array of strings");
        }
        ArrayList<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text)) {
                throw new IOException("Expected string array item");
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    private static Map<String, String> readStringMap(Map<String, Object> raw) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String field) throws IOException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException(field + " must be an object");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionalObject(Object value) throws IOException {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException("Expected object");
        }
        return (Map<String, Object>) map;
    }

    private static String string(Object value, String field) throws IOException {
        if (!(value instanceof String text)) {
            throw new IOException(field + " must be a string");
        }
        return text;
    }

    private static String stringOrDefault(Object value, String defaultValue) throws IOException {
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String text)) {
            throw new IOException("Expected string value");
        }
        return text;
    }

    private static Instant instantOrNow(Object value) throws IOException {
        if (value == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(string(value, "instant"));
        } catch (RuntimeException ex) {
            throw new IOException("Invalid ISO-8601 instant", ex);
        }
    }

    private static <E extends Enum<E>> E enumOrDefault(Class<E> enumType, String value, E fallback) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static String toJson(NarrationScriptDocument script) {
        StringBuilder out = new StringBuilder(16384);
        out.append("{\n");
        field(out, 1, "id", quote(script.id()), true);
        field(out, 1, "title", quote(script.title()), true);
        field(out, 1, "language", quote(script.language()), true);
        field(out, 1, "sourceDocumentTitle", quote(script.sourceDocumentTitle()), true);
        field(out, 1, "segmentCount", Integer.toString(script.segmentCount()), true);
        field(out, 1, "narratableSegmentCount", Long.toString(script.narratableSegmentCount()), true);
        field(out, 1, "estimatedCharacters", Long.toString(script.estimatedCharacters()), true);
        field(out, 1, "wordCount", Long.toString(script.wordCount()), true);
        field(out, 1, "createdAt", quote(script.createdAt().toString()), true);
        field(out, 1, "updatedAt", quote(script.updatedAt().toString()), true);
        field(out, 1, "notes", quote(script.notes()), true);
        writeSegments(out, script);
        out.append("}\n");
        return out.toString();
    }

    private static void writeSegments(StringBuilder out, NarrationScriptDocument script) {
        indent(out, 1).append("\"segments\": [");
        if (!script.segments().isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < script.segments().size(); i++) {
            NarrationSegment segment = script.segments().get(i);
            indent(out, 2).append("{\n");
            field(out, 3, "id", quote(segment.id()), true);
            field(out, 3, "type", quote(segment.type().name()), true);
            field(out, 3, "title", quote(segment.title()), true);
            field(out, 3, "narrationText", quote(segment.narrationText()), true);
            stringArrayField(out, 3, "sourceBlockIds", segment.sourceBlockIds()); out.append(",\n");
            field(out, 3, "characterId", quote(segment.characterId()), true);
            field(out, 3, "voiceProfileId", quote(segment.voiceProfileId()), true);
            field(out, 3, "performanceStyleId", quote(segment.performanceStyleId()), true);
            indent(out, 3).append("\"metadata\": {");
            if (!segment.metadata().isEmpty()) out.append("\n");
            int index = 0;
            for (var entry : segment.metadata().entrySet()) {
                indent(out, 4).append(quote(entry.getKey())).append(": ").append(quote(entry.getValue()));
                if (index < segment.metadata().size() - 1) out.append(",");
                out.append("\n");
                index++;
            }
            if (!segment.metadata().isEmpty()) indent(out, 3);
            out.append("}\n");
            indent(out, 2).append("}");
            if (i < script.segments().size() - 1) out.append(",");
            out.append("\n");
        }
        indent(out, 1).append("]\n");
    }

    private static void stringArrayField(StringBuilder out, int level, String name, java.util.List<String> values) {
        indent(out, level).append(quote(name)).append(": [");
        for (int i = 0; i < values.size(); i++) {
            out.append(quote(values.get(i)));
            if (i < values.size() - 1) out.append(", ");
        }
        out.append("]");
    }

    private static void field(StringBuilder out, int level, String name, String value, boolean comma) {
        indent(out, level).append(quote(name)).append(": ").append(value);
        if (comma) out.append(",");
        out.append("\n");
    }

    private static StringBuilder indent(StringBuilder out, int level) {
        return out.append("    ".repeat(level));
    }

    private static String quote(String value) {
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
                    if (c < 0x20) escaped.append(String.format("\\u%04x", (int) c));
                    else escaped.append(c);
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
