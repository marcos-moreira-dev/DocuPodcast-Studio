package com.marcosmoreiradev.docupodcaststudio.domain.reading;

import java.util.Locale;

/** Languages offered by the V1 document listening adaptation. */
public enum DocumentListeningLanguage {
    SPANISH("es", "Español"),
    ENGLISH("en", "English");

    private final String tag;
    private final String label;

    DocumentListeningLanguage(String tag, String label) {
        this.tag = tag;
        this.label = label;
    }

    public String tag() { return tag; }

    public static DocumentListeningLanguage fromTag(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return normalized.startsWith("en") ? ENGLISH : SPANISH;
    }

    @Override public String toString() { return label; }
}
