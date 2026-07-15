package com.marcosmoreiradev.docupodcaststudio.domain.audio;

/** Per-segment generation state. */
public enum AudioSegmentStatus {
    PENDING("Pendiente"),
    GENERATING("Generando"),
    COMPLETED("Completado"),
    FAILED("Fallido"),
    SKIPPED("Omitido"),
    CANCELLED("Cancelado");

    private final String displayName;

    AudioSegmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
