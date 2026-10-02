package com.marcosmoreiradev.docupodcaststudio.domain.reading;

/** How table notices should be treated before the script is generated. */
public enum TableNarrationPolicy {
    READ_ALL("Leer toda la tabla"),
    READ_TEXTUAL_CONTENT("Leer el contenido textual"),
    SUMMARIZE("Resumir estructura y datos verificables"),
    ANNOUNCE_ONLY("Anunciar únicamente la tabla"),
    SKIP("Omitir la tabla"),

    ANNOUNCE_SUMMARY("Anunciar o resumir cuando sea útil"),
    READ_STRUCTURED("Leer la estructura completa"),
    IGNORE_TABLES("Excluir cuadros y tablas");

    private final String displayName;

    TableNarrationPolicy(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Maps persisted legacy constants to the five effective A6 policies. */
    public TableNarrationPolicy canonical() {
        return switch (this) {
            case ANNOUNCE_SUMMARY -> SUMMARIZE;
            case READ_STRUCTURED -> READ_ALL;
            case IGNORE_TABLES -> SKIP;
            default -> this;
        };
    }

    public boolean skips() {
        return canonical() == SKIP;
    }

    public boolean readsAll() {
        return canonical() == READ_ALL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
