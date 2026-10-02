package com.marcosmoreiradev.docupodcaststudio.application.script;

import java.util.Objects;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Theatre/dialogue helper that lets users omit a speaker label before the first colon.
 *
 * <p>Example: {@code Vaquero: Voy a conquistar el viejo oeste} becomes
 * {@code Voy a conquistar el viejo oeste} for generated narration. The source document is not
 * modified; this policy only changes the internal audio projection when the user enables it.</p>
 */
public final class ReadAfterColonTextPolicy {
    private static final Set<String> CONNECTORS = Set.of(
            "de", "del", "la", "las", "el", "los", "y", "e", "en", "al");
    private static final Set<String> NATURAL_SENTENCE_PREFIXES = Set.of(
            "si", "no", "porque", "cuando", "donde", "como", "entonces", "pero",
            "nota", "ejemplo", "resumen", "resultado", "advertencia", "importante");
    private ReadAfterColonTextPolicy() {
    }

    public static String narrationText(String sourceText, boolean enabled) {
        String text = Objects.toString(sourceText, "").strip();
        if (!enabled) {
            return text;
        }
        int colon = text.indexOf(':');
        if (colon < 1 || colon >= text.length() - 1) {
            return text;
        }
        String prefix = text.substring(0, colon).strip();
        String suffix = text.substring(colon + 1).strip();
        if (!looksLikeSpeakerLabel(prefix) || suffix.isBlank()) {
            return text;
        }
        return suffix;
    }

    private static boolean looksLikeSpeakerLabel(String prefix) {
        if (prefix.isBlank() || prefix.length() > 48
                || prefix.matches(".*[.!?¿¡;].*")) return false;
        String[] words = prefix.split("\\s+");
        if (words.length == 0 || words.length > 8) return false;
        String normalizedFirst = normalize(words[0]);
        if (NATURAL_SENTENCE_PREFIXES.contains(normalizedFirst)) return false;
        for (String raw : words) {
            String word = raw.replaceAll("^[\\p{Punct}&&[^_-]]+|[\\p{Punct}&&[^_-]]+$", "");
            if (word.isBlank() || word.chars().allMatch(Character::isDigit)) continue;
            String normalized = normalize(word);
            if (CONNECTORS.contains(normalized)) continue;
            int firstLetter = word.codePoints().filter(Character::isLetter).findFirst().orElse(-1);
            if (firstLetter < 0 || (!Character.isUpperCase(firstLetter)
                    && !word.equals(word.toUpperCase(Locale.ROOT)))) return false;
        }
        return true;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
