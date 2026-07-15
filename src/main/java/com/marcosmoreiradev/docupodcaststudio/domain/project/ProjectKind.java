package com.marcosmoreiradev.docupodcaststudio.domain.project;

/**
 * Coarse-grained project kind used to validate which payload sections must exist.
 */
public enum ProjectKind {
    EMPTY("Proyecto vacío"),
    DOCUMENT_ONLY("Documento importado"),
    NARRATION_SCRIPT("Lectura preparada"),
    STORYBOARD("Secuencia visual"),
    AUDIO_PROJECT("Proyecto de audio"),
    FULL_PROJECT("Proyecto completo");

    private final String displayName;

    ProjectKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
