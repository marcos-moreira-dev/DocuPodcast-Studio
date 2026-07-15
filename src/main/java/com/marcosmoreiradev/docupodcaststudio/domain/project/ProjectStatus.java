package com.marcosmoreiradev.docupodcaststudio.domain.project;

/** Lifecycle status for a DocuPodcast project. */
public enum ProjectStatus {
    DRAFT("Borrador"),
    DOCUMENT_IMPORTED("Documento importado"),
    SCRIPT_READY("Lectura lista"),
    STORYBOARD_READY("Storyboard listo"),
    AUDIO_IN_PROGRESS("Audio en progreso"),
    AUDIO_READY("Audio listo"),
    EXPORT_READY("Listo para exportar");

    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
