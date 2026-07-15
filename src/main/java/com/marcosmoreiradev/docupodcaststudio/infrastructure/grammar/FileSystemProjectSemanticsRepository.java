package com.marcosmoreiradev.docupodcaststudio.infrastructure.grammar;

import com.marcosmoreiradev.docupodcaststudio.application.grammar.GrammarDiagnostic;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ProjectSemanticsDocument;
import com.marcosmoreiradev.docupodcaststudio.application.grammar.ProjectSemanticsRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

/** File-system repository for the project-semantics.json sidecar. */
public final class FileSystemProjectSemanticsRepository implements ProjectSemanticsRepository {
    public static final String FILE_NAME = "project-semantics.json";

    @Override
    public void save(Path projectDirectory, ProjectSemanticsDocument document) throws IOException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Objects.requireNonNull(document, "document");
        Path normalizedRoot = projectDirectory.toAbsolutePath().normalize();
        Files.createDirectories(normalizedRoot);
        Files.writeString(normalizedRoot.resolve(FILE_NAME), toJson(document), StandardCharsets.UTF_8);
    }

    private static String toJson(ProjectSemanticsDocument document) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        field(out, 1, "schemaVersion", Integer.toString(document.schemaVersion()), false, true);
        field(out, 1, "projectMode", document.projectMode().name(), true, true);
        field(out, 1, "grammarKind", document.grammarKind().name(), true, true);
        field(out, 1, "grammarVersion", document.grammarVersion(), true, true);
        field(out, 1, "sourceFileName", document.sourceFileName(), true, true);
        field(out, 1, "importedAt", DateTimeFormatter.ISO_INSTANT.format(document.importedAt()), true, true);
        indent(out, 1).append("\"fragmentBindings\": [");
        if (!document.fragmentBindings().isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < document.fragmentBindings().size(); i++) {
            ProjectSemanticsDocument.FragmentBinding binding = document.fragmentBindings().get(i);
            indent(out, 2).append("{\n");
            field(out, 3, "fragmentId", binding.fragmentId(), true, true);
            field(out, 3, "segmentId", binding.segmentId(), true, true);
            field(out, 3, "blockId", binding.blockId(), true, true);
            field(out, 3, "title", binding.title(), true, true);
            field(out, 3, "semanticType", binding.semanticType(), true, true);
            mapField(out, 3, "metadata", binding.metadata(), false);
            indent(out, 2).append("}");
            if (i < document.fragmentBindings().size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        if (!document.fragmentBindings().isEmpty()) {
            indent(out, 1);
        }
        out.append("],\n");
        mapField(out, 1, "narrativeSummary", document.narrativeSummary(), true);
        mapField(out, 1, "theatreSummary", document.theatreSummary(), true);
        diagnosticsField(out, 1, document.diagnostics());
        out.append("\n}\n");
        return out.toString();
    }

    private static void diagnosticsField(StringBuilder out, int level, java.util.List<GrammarDiagnostic> diagnostics) {
        indent(out, level).append("\"diagnostics\": [");
        if (!diagnostics.isEmpty()) {
            out.append("\n");
        }
        for (int i = 0; i < diagnostics.size(); i++) {
            GrammarDiagnostic diagnostic = diagnostics.get(i);
            indent(out, level + 1).append("{\n");
            field(out, level + 2, "severity", diagnostic.severity().name(), true, true);
            field(out, level + 2, "code", diagnostic.code(), true, true);
            field(out, level + 2, "message", diagnostic.message(), true, true);
            field(out, level + 2, "detail", diagnostic.detail(), true, false);
            indent(out, level + 1).append("}");
            if (i < diagnostics.size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        if (!diagnostics.isEmpty()) {
            indent(out, level);
        }
        out.append("]");
    }

    private static void mapField(StringBuilder out, int level, String name, Map<String, String> values, boolean comma) {
        indent(out, level).append("\"").append(escape(name)).append("\": {");
        if (values != null && !values.isEmpty()) {
            out.append("\n");
            int index = 0;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                field(out, level + 1, entry.getKey(), entry.getValue(), true, index < values.size() - 1);
                index++;
            }
            indent(out, level);
        }
        out.append("}");
        if (comma) {
            out.append(",");
        }
        out.append("\n");
    }

    private static void field(StringBuilder out, int level, String name, String value, boolean quoted, boolean comma) {
        indent(out, level).append("\"").append(escape(name)).append("\": ");
        if (quoted) {
            out.append("\"").append(escape(value)).append("\"");
        } else {
            out.append(value);
        }
        if (comma) {
            out.append(",");
        }
        out.append("\n");
    }

    private static StringBuilder indent(StringBuilder out, int level) {
        return out.append("  ".repeat(Math.max(0, level)));
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        StringBuilder out = new StringBuilder(safe.length() + 8);
        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }
}
