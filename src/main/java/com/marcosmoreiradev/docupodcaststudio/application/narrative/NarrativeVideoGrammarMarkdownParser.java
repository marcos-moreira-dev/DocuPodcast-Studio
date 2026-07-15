package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parser for the lightweight Video narrativo Markdown grammar. */
public final class NarrativeVideoGrammarMarkdownParser {
    private NarrativeVideoGrammarMarkdownParser() {
    }

    public static NarrativeVideoImportPlan parse(Path path) throws IOException {
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static NarrativeVideoImportPlan parse(String markdown) {
        String title = "Video narrativo";
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        ArrayList<NarrativeVideoImportPlan.FragmentPlan> fragments = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        FragmentBuilder current = null;
        String activeMultiline = "";
        boolean metadataSection = false;

        String[] lines = (markdown == null ? "" : markdown).replace("\r\n", "\n").split("\n", -1);
        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.equalsIgnoreCase("## Metadatos") || line.equalsIgnoreCase("## Metadata")) {
                metadataSection = true;
                activeMultiline = "";
                continue;
            }
            if (line.startsWith("# ") && !line.toLowerCase(Locale.ROOT).contains("docupodcast")) {
                String headingTitle = line.substring(2).strip();
                if (!headingTitle.isBlank()) {
                    title = headingTitle;
                }
                continue;
            }
            if (line.startsWith("### ") || line.matches("^##\\s+Fragmento\\b.*")) {
                if (current != null) {
                    fragments.add(current.build());
                }
                current = new FragmentBuilder(fragments.size() + 1);
                current.title = fragmentTitle(line.replaceFirst("^#+\\s*", "").strip(), current.order);
                activeMultiline = "";
                metadataSection = false;
                continue;
            }
            if (metadataSection && line.startsWith("- ")) {
                parseMetadataLine(metadata, line.substring(2));
                continue;
            }
            if (current != null && line.startsWith(">")) {
                activeMultiline = applyMetadataDirective(current, line.substring(1).strip());
                continue;
            }
            int sep = line.indexOf(':');
            if (sep >= 0) {
                String key = key(line.substring(0, sep));
                String value = line.substring(sep + 1).strip();
                if (metadataSection && current == null) {
                    metadata.put(key(line.substring(0, sep)), value);
                    continue;
                }
                activeMultiline = applyKey(current, key, value);
                if (current == null && key.equals("titulo") && !value.isBlank()) {
                    title = value;
                }
                continue;
            }
            if (current != null && activeMultiline.equals("texto")) {
                current.appendText(rawLine);
                continue;
            }
            if (current != null && !line.isBlank() && activeMultiline.isBlank()
                    && !line.startsWith("#") && !line.startsWith("- ")) {
                current.appendText(rawLine);
            }
        }
        if (current != null) {
            fragments.add(current.build());
        }
        if (fragments.isEmpty()) {
            warnings.add("No se encontraron fragmentos narrativos en la gramatica.");
        }
        return new NarrativeVideoImportPlan(title, metadata, fragments, warnings);
    }

    private static String applyKey(FragmentBuilder current, String key, String value) {
        if (current == null) {
            return "";
        }
        switch (key) {
            case "titulo" -> current.title = value;
            case "promptvisual", "promptimagen", "visualprompt", "visual" -> current.visualPrompt = value;
            case "promptpuente", "bridgeprompt", "imagenpuente" -> current.bridgePrompt = value;
            case "notas", "nota" -> current.notes = value;
            case "tono", "tone" -> current.tone = value;
            case "corte", "cortefuerte" -> current.hardCut = value.equalsIgnoreCase("fuerte")
                    || value.equalsIgnoreCase("true")
                    || value.equalsIgnoreCase("si");
            case "texto" -> {
                current.text.setLength(0);
                if (!value.isBlank()) {
                    current.text.append(value);
                }
                return "texto";
            }
            default -> {
                return "";
            }
        }
        return "";
    }

    private static String applyMetadataDirective(FragmentBuilder current, String directive) {
        int sep = directive.indexOf('=');
        if (sep < 0) {
            sep = directive.indexOf(':');
        }
        if (sep < 0) {
            return "";
        }
        return applyKey(current, key(directive.substring(0, sep)), directive.substring(sep + 1).strip());
    }

    private static void parseMetadataLine(Map<String, String> metadata, String line) {
        int sep = line.indexOf(':');
        if (sep < 0) {
            sep = line.indexOf('=');
        }
        if (sep < 0) {
            return;
        }
        metadata.put(key(line.substring(0, sep)), line.substring(sep + 1).strip());
    }

    private static String fragmentTitle(String rawTitle, int order) {
        String normalized = rawTitle == null ? "" : rawTitle.strip();
        return normalized.isBlank() ? "Fragmento " + order : normalized;
    }

    private static String key(String raw) {
        String ascii = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static final class FragmentBuilder {
        private final int order;
        private String title = "";
        private String visualPrompt = "";
        private String bridgePrompt = "";
        private boolean hardCut;
        private String notes = "";
        private String tone = "";
        private final StringBuilder text = new StringBuilder();

        private FragmentBuilder(int order) {
            this.order = order;
        }

        private void appendText(String line) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(line.strip());
        }

        private NarrativeVideoImportPlan.FragmentPlan build() {
            return new NarrativeVideoImportPlan.FragmentPlan(
                    order,
                    title,
                    text.toString(),
                    visualPrompt,
                    bridgePrompt,
                    hardCut,
                    notes,
                    tone);
        }
    }
}
