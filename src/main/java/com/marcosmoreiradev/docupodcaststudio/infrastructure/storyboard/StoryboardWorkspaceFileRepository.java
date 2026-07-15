package com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.MaterializedStoryboard;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.StoryboardWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;

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

/** Writes storyboard/storyboard.json as a portable manifest beside the project file. */
public final class StoryboardWorkspaceFileRepository implements StoryboardWorkspaceRepository {
    private static final String STORYBOARD_PATH = "storyboard/storyboard.json";

    @Override
    public MaterializedStoryboard materialize(Path projectFile, StoryboardDocument storyboard) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(storyboard, "storyboard");
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            throw new IOException("Project file must have a parent directory");
        }
        Path target = projectDirectory.resolve(STORYBOARD_PATH).normalize();
        if (!target.startsWith(projectDirectory.resolve("storyboard").normalize())) {
            throw new IOException("Invalid storyboard target path");
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, write(storyboard), StandardCharsets.UTF_8);
        ProjectAssetReference asset = new ProjectAssetReference(
                "STORYBOARD-001",
                ProjectAssetKind.STORYBOARD_MANIFEST,
                "Secuencia visual",
                STORYBOARD_PATH,
                "application/json",
                "Manifest de panel visual asociado a la lectura preparada",
                "",
                ""
        );
        return new MaterializedStoryboard(target, asset);
    }


    @Override
    public Optional<StoryboardDocument> load(Path projectFile) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            throw new IOException("Project file must have a parent directory");
        }
        Path source = projectDirectory.resolve(STORYBOARD_PATH).normalize();
        if (!Files.isRegularFile(source)) {
            return Optional.empty();
        }
        Object parsed = SimpleJsonParser.parse(Files.readString(source, StandardCharsets.UTF_8));
        Map<String, Object> map = object(parsed, STORYBOARD_PATH);
        return Optional.of(new StoryboardDocument(
                stringOrDefault(map.get("id"), "STORYBOARD-001"),
                stringOrDefault(map.get("title"), "Secuencia visual"),
                stringOrDefault(map.get("sourceScriptId"), "SCRIPT-001"),
                readBindings(map.get("bindings")),
                readStringMap(optionalObject(map.get("layout"))),
                instantOrNow(map.get("createdAt")),
                instantOrNow(map.get("updatedAt")),
                stringOrDefault(map.get("notes"), "")
        ));
    }

    private static List<StoryboardBinding> readBindings(Object value) throws IOException {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IOException("storyboard.bindings must be an array");
        }
        ArrayList<StoryboardBinding> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> binding = object(item, "storyboard.bindings[]");
            result.add(new StoryboardBinding(
                    string(binding.get("id"), "binding.id"),
                    string(binding.get("segmentId"), "binding.segmentId"),
                    string(binding.get("imageAssetId"), "binding.imageAssetId"),
                    enumOrDefault(StoryboardDisplayMode.class, stringOrDefault(binding.get("displayMode"), StoryboardDisplayMode.FIT_CONTAIN.name()), StoryboardDisplayMode.FIT_CONTAIN),
                    stringOrDefault(binding.get("caption"), ""),
                    readStringMap(optionalObject(binding.get("metadata")))
            ));
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

    public String write(StoryboardDocument storyboard) {
        StringBuilder out = new StringBuilder(4096);
        out.append("{\n");
        field(out, 1, "id", quote(storyboard.id())); out.append(",\n");
        field(out, 1, "title", quote(storyboard.title())); out.append(",\n");
        field(out, 1, "sourceScriptId", quote(storyboard.sourceScriptId())); out.append(",\n");
        field(out, 1, "createdAt", quote(storyboard.createdAt().toString())); out.append(",\n");
        field(out, 1, "updatedAt", quote(storyboard.updatedAt().toString())); out.append(",\n");
        field(out, 1, "notes", quote(storyboard.notes())); out.append(",\n");
        indent(out, 1).append("\"bindings\": [");
        if (!storyboard.bindings().isEmpty()) { out.append("\n"); }
        for (int i = 0; i < storyboard.bindings().size(); i++) {
            StoryboardBinding binding = storyboard.bindings().get(i);
            indent(out, 2).append("{\n");
            field(out, 3, "id", quote(binding.id())); out.append(",\n");
            field(out, 3, "segmentId", quote(binding.segmentId())); out.append(",\n");
            field(out, 3, "imageAssetId", quote(binding.imageAssetId())); out.append(",\n");
            field(out, 3, "displayMode", quote(binding.displayMode().name())); out.append(",\n");
            field(out, 3, "caption", quote(binding.caption())); out.append("\n");
            indent(out, 2).append("}");
            if (i < storyboard.bindings().size() - 1) { out.append(","); }
            out.append("\n");
        }
        indent(out, 1).append("]\n");
        out.append("}\n");
        return out.toString();
    }

    private static void field(StringBuilder out, int level, String name, String value) {
        indent(out, level).append(quote(name)).append(": ").append(value);
    }

    private static StringBuilder indent(StringBuilder out, int level) {
        return out.append("  ".repeat(Math.max(0, level)));
    }

    private static String quote(String value) {
        String text = value == null ? "" : value;
        StringBuilder out = new StringBuilder(text.length() + 2);
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(ch);
            }
        }
        out.append('"');
        return out.toString();
    }
}
