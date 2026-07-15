package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** How a voice reference sample entered DocuPodcast. */
public enum VoiceSampleOrigin {
    RECORDED_IN_APP("Grabada en la aplicación"),
    IMPORTED_FILE("Importada desde archivo"),
    APP_DEFAULT("Referencia prediseñada"),
    GENERATED_CACHE("Artefacto generado/cacheado");

    private final String displayName;

    VoiceSampleOrigin(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
