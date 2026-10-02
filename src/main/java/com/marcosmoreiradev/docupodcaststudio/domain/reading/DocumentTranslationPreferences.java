package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Persisted product preference: adapt spoken narration, never the source asset. */
public record DocumentTranslationPreferences(
        boolean enabled,
        DocumentListeningLanguage listeningLanguage) {
    public static final String ENABLED_KEY = "documentTranslation.enabled";
    public static final String LANGUAGE_KEY = "documentTranslation.listeningLanguage";

    public DocumentTranslationPreferences {
        listeningLanguage = Objects.requireNonNullElse(
                listeningLanguage, DocumentListeningLanguage.SPANISH);
    }

    public static DocumentTranslationPreferences defaults() {
        return new DocumentTranslationPreferences(false, DocumentListeningLanguage.SPANISH);
    }

    public static DocumentTranslationPreferences fromViewState(Map<String, String> state) {
        Map<String, String> safe = state == null ? Map.of() : state;
        return new DocumentTranslationPreferences(
                Boolean.parseBoolean(safe.getOrDefault(ENABLED_KEY, "false")),
                DocumentListeningLanguage.fromTag(safe.get(LANGUAGE_KEY)));
    }

    public Map<String, String> applyTo(Map<String, String> state) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(
                state == null ? Map.of() : state);
        result.put(ENABLED_KEY, Boolean.toString(enabled));
        result.put(LANGUAGE_KEY, listeningLanguage.tag());
        return Map.copyOf(result);
    }

    public DocumentTranslationPreferences withEnabled(boolean value) {
        return new DocumentTranslationPreferences(value, listeningLanguage);
    }

    public DocumentTranslationPreferences withListeningLanguage(
            DocumentListeningLanguage value) {
        return new DocumentTranslationPreferences(enabled, value);
    }
}
