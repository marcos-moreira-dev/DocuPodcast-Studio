package com.marcosmoreiradev.docupodcaststudio.domain.theatre;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Canonical vocabulary for theatre interaction targets across import, editing and rendering. */
public final class TheatreInteractionTargetPolicy {
    public static final String ALL_REMAINING = "Todos los personajes restantes";

    private static final Set<String> ALL_REMAINING_ALIASES = Set.of(
            "grupo",
            "el grupo",
            "todo el grupo",
            "todos los presentes",
            "los demas personajes",
            "personajes restantes",
            "todos los personajes restantes");

    private TheatreInteractionTargetPolicy() {
    }

    public static String canonicalize(String target) {
        String value = target == null ? "" : target.strip();
        return isAllRemaining(value) ? ALL_REMAINING : value;
    }

    public static boolean isAllRemaining(String target) {
        return ALL_REMAINING_ALIASES.contains(normalize(target));
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}
