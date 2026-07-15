package com.marcosmoreiradev.docupodcaststudio.application.audio;

/** Operational health of a persisted audio job when reopening or refreshing a project. */
public enum AudioJobHealthStatus {
    READY("Listo para reproducir"),
    RESUMABLE("Reanudable"),
    STALE_SOURCE("Obsoleto por cambios en la fuente"),
    MISSING_AUDIO("Faltan archivos de audio"),
    EMPTY_JOB("Sin segmentos de audio"),
    REVIEW_REQUIRED("Requiere revisión");

    private final String displayName;

    AudioJobHealthStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
