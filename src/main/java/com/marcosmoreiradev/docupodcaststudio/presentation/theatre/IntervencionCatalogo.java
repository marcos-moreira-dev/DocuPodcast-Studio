package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Presentation-level intervention catalog for the theatrical sequence. */
public final class IntervencionCatalogo {
    private IntervencionCatalogo() {
    }

    public static List<IntervencionInfo> intervenciones(ReadableDocument document, NarrationScriptDocument script) {
        if (document == null) {
            return List.of();
        }
        if (script != null && !script.empty()) {
            return intervencionesDesdeScript(document, script);
        }
        return intervencionesDesdeBloques(document);
    }

    private static List<IntervencionInfo> intervencionesDesdeScript(ReadableDocument document, NarrationScriptDocument script) {
        ArrayList<IntervencionInfo> aliases = new ArrayList<>();
        Map<String, String> blockTextById = new LinkedHashMap<>();
        for (DocumentBlock block : document.blocks()) {
            if (block != null && block.id() != null && !block.id().isBlank()) {
                blockTextById.put(block.id(), block.text());
            }
        }
        Set<String> seenBlocks = new LinkedHashSet<>();
        int index = 1;
        for (NarrationSegment segment : script.segments()) {
            if (segment == null || !segment.narratable()) {
                continue;
            }
            String blockId = segment.sourceBlockIds().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse("");
            String cueText = blockTextById.getOrDefault(blockId, segment.narrationText());
            if (!esCueTeatral(cueText)) {
                continue;
            }
            if (blockId.isBlank() || !seenBlocks.add(blockId)) {
                continue;
            }
            aliases.add(new IntervencionInfo("INTERVENCION-" + index, "Intervencion " + index, blockId, preview(cueText), fullText(cueText)));
            index++;
        }
        return List.copyOf(aliases);
    }

    private static List<IntervencionInfo> intervencionesDesdeBloques(ReadableDocument document) {
        ArrayList<IntervencionInfo> aliases = new ArrayList<>();
        int index = 1;
        for (DocumentBlock block : document.blocks()) {
            if (block == null || !block.narratable() || !esCueTeatral(block.text())) {
                continue;
            }
            aliases.add(new IntervencionInfo("INTERVENCION-" + index, "Intervencion " + index, block.id(), preview(block.text()), fullText(block.text())));
            index++;
        }
        return List.copyOf(aliases);
    }

    static boolean esCueTeatral(String text) {
        String normalized = normalizeCue(text);
        if (normalized.isBlank()) {
            return false;
        }
        if (normalized.startsWith("ACOTACION:")
                || normalized.startsWith("ACOTACION ")
                || normalized.startsWith("(")
                || normalized.startsWith("[")) {
            return true;
        }
        int colon = normalized.indexOf(':');
        if (colon < 2 || colon > 42) {
            return false;
        }
        String cue = normalized.substring(0, colon).strip();
        if (cue.equals("ESCENA") || cue.startsWith("ESCENA ")
                || cue.equals("ACTO") || cue.startsWith("ACTO ")) {
            return false;
        }
        boolean hasLetter = cue.chars().anyMatch(Character::isLetter);
        boolean legalCue = cue.chars().allMatch(ch ->
                Character.isLetterOrDigit(ch) || ch == ' ' || ch == '.' || ch == '-' || ch == '_' || ch == '\'');
        return hasLetter && legalCue;
    }

    private static String normalizeCue(String text) {
        String value = text == null ? "" : text.replace('\u00A0', ' ').strip();
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "")
                .replaceAll("[\\s\\u00A0]+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private static String preview(String text) {
        String normalized = text == null ? "" : text.strip();
        if (normalized.length() <= 96) {
            return normalized;
        }
        return normalized.substring(0, 96).strip() + "...";
    }

    private static String fullText(String text) {
        return text == null ? "" : text.strip();
    }

    public record IntervencionInfo(String alias, String displayName, String blockId, String preview, String fullText) {
    }
}
