package com.marcosmoreiradev.docupodcaststudio.domain.audio;

/** Coarse stage shown to the user while an audio job runs. */
public enum AudioGenerationStage {
    NONE("Sin etapa"),
    PREPARING_WORKSPACE("Preparando carpeta de trabajo"),
    GENERATING_SEGMENTS("Generando segmentos"),
    MERGING_SEGMENTS("Uniendo segmentos"),
    EXPORT_READY("Salida lista"),
    CANCELLED("Cancelado"),
    FAILED("Fallido");

    private final String displayName;

    AudioGenerationStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
