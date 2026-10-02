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
        Map<String, String> blockInterventionIdById = new LinkedHashMap<>();
        for (DocumentBlock block : document.blocks()) {
            if (block != null && block.id() != null && !block.id().isBlank()) {
                blockTextById.put(block.id(), block.text());
                String declaredId = block.metadata().getOrDefault("theatreGrammarInterventionId", "");
                if (!declaredId.isBlank()) {
                    blockInterventionIdById.put(block.id(), declaredId);
                }
            }
        }
        Set<String> seenBlocks = new LinkedHashSet<>();
        int index = 1;
        for (NarrationSegment segment : script.segments()) {
            if (segment == null || (!segment.narratable() && !stageDirection(segment))) {
                continue;
            }
            String blockId = segment.sourceBlockIds().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse("");
            String cueText = blockTextById.getOrDefault(blockId, segment.narrationText());
            String declaredId = canonicalInterventionId(
                    blockId,
                    blockInterventionIdById.getOrDefault(blockId, ""),
                    segment.metadata().getOrDefault("theatreGrammarInterventionId", ""));
            if (declaredId.isBlank() && !esCueTeatral(cueText)) {
                continue;
            }
            if (blockId.isBlank() || !seenBlocks.add(blockId)) {
                continue;
            }
            aliases.add(new IntervencionInfo(declaredId.isBlank() ? "INTERVENCION-" + index : declaredId,
                    "Intervencion " + index, blockId, preview(cueText), fullText(cueText),
                    stageDirection(segment)));
            index++;
        }
        return List.copyOf(aliases);
    }

    private static List<IntervencionInfo> intervencionesDesdeBloques(ReadableDocument document) {
        ArrayList<IntervencionInfo> aliases = new ArrayList<>();
        int index = 1;
        for (DocumentBlock block : document.blocks()) {
            if (block == null || (!block.narratable() && !stageDirection(block))) {
                continue;
            }
            String declaredId = canonicalInterventionId(
                    block.id(),
                    block.metadata().getOrDefault("theatreGrammarInterventionId", ""),
                    "");
            if (declaredId.isBlank() && !esCueTeatral(block.text())) continue;
            aliases.add(new IntervencionInfo(declaredId.isBlank() ? "INTERVENCION-" + index : declaredId,
                    "Intervencion " + index, block.id(), preview(block.text()), fullText(block.text()),
                    stageDirection(block)));
            index++;
        }
        return List.copyOf(aliases);
    }

    /**
     * Keeps the document block as the stable identity of a theatrical intervention.
     * Narration scripts are derived artifacts and may temporarily retain stale grammar
     * metadata after the canonical theatre Markdown is refreshed.
     */
    static String canonicalInterventionId(String blockId, String blockDeclaredId, String scriptDeclaredId) {
        String normalizedBlockId = blockId == null ? "" : blockId.strip();
        String prefix = "B-INTERVENCION-";
        if (normalizedBlockId.regionMatches(true, 0, prefix, 0, prefix.length())) {
            String suffix = normalizedBlockId.substring(prefix.length()).strip();
            if (!suffix.isBlank() && suffix.chars().allMatch(Character::isDigit)) {
                return "INTERVENCION-" + suffix;
            }
        }
        String blockValue = blockDeclaredId == null ? "" : blockDeclaredId.strip();
        if (!blockValue.isBlank()) {
            return blockValue;
        }
        return scriptDeclaredId == null ? "" : scriptDeclaredId.strip();
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

    private static boolean stageDirection(NarrationSegment segment) {
        return segment != null && Boolean.parseBoolean(
                segment.metadata().getOrDefault("theatreStageDirection", "false"));
    }

    private static boolean stageDirection(DocumentBlock block) {
        return block != null && Boolean.parseBoolean(
                block.metadata().getOrDefault("theatreStageDirection", "false"));
    }

    public record IntervencionInfo(String alias, String displayName, String blockId,
                                   String preview, String fullText, boolean stageDirection) {
        public IntervencionInfo(String alias, String displayName, String blockId,
                                String preview, String fullText) {
            this(alias, displayName, blockId, preview, fullText, false);
        }
    }
}
