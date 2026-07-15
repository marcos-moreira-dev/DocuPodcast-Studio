package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** Grouping used by the voice-registration wizard to avoid overwhelming casual users. */
public enum VoiceReferenceToneCategory {
    BASIC("Tonos recomendados"),
    THEATRICAL_EXTENDED("Catálogo teatral extendido");

    private final String displayName;

    VoiceReferenceToneCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
