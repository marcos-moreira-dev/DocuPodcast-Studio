package com.marcosmoreiradev.docupodcaststudio.application.export;

/** Stable identifiers for export outputs owned by the application brain. */
public enum ExportableArtifactKind {
    PROJECT_BUNDLE("Paquete completo auditable"),
    DIAGNOSTIC_REPORT("Reporte diagnóstico"),
    PODCAST_WAV("Podcast WAV / Audio final WAV"),
    DOCUMENT_TEXT_AUDIO_VIDEO("Video documental texto+audio"),
    THEATRE_WORK_VIDEO("Video teatral limpio"),
    THEATRE_SPATIAL_MAP_VIDEO("Video de mapa teatral"),
    THEATRE_PORTION_VIDEO("Video de acto o escena teatral"),
    FINAL_VIDEO_MP4("Video MP4 final"),
    STORYBOARD_SUMMARY("Panel visual / imágenes"),
    SIMPLE_VIDEO_PACKAGE("Paquete de video simple auditable");

    private final String displayName;

    ExportableArtifactKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
