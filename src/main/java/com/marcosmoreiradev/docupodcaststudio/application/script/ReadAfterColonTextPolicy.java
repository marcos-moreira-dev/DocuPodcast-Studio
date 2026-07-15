package com.marcosmoreiradev.docupodcaststudio.application.script;

import java.util.Objects;

/**
 * Theatre/dialogue helper that lets users omit a speaker label before the first colon.
 *
 * <p>Example: {@code Vaquero: Voy a conquistar el viejo oeste} becomes
 * {@code Voy a conquistar el viejo oeste} for generated narration. The source document is not
 * modified; this policy only changes the internal audio projection when the user enables it.</p>
 */
public final class ReadAfterColonTextPolicy {
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
        if (prefix.isBlank() || suffix.isBlank() || prefix.length() > 48 || prefix.contains(".")) {
            return text;
        }
        return suffix;
    }
}
