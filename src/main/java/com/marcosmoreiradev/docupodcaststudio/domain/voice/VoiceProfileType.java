package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** Origin and usage boundary of a voice profile. */
public enum VoiceProfileType {
    PREDEFINED("Voz prediseñada"),
    OWN("Voz propia"),
    AUTHORIZED("Voz autorizada"),
    IMPORTED("Voz importada"),
    UNKNOWN("Sin clasificar");

    private final String displayName;

    VoiceProfileType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean requiresConsentNote() {
        return this == AUTHORIZED || this == IMPORTED;
    }
}
