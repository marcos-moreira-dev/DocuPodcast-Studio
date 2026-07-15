package com.marcosmoreiradev.docupodcaststudio.domain.reading;

/** How table notices should be treated before the script is generated. */
public enum TableNarrationPolicy {
    ANNOUNCE_SUMMARY("Leer resumen simple de tabla"),
    READ_STRUCTURED("Leer tabla estructurada"),
    IGNORE_TABLES("Ignorar tablas");

    private final String displayName;

    TableNarrationPolicy(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
