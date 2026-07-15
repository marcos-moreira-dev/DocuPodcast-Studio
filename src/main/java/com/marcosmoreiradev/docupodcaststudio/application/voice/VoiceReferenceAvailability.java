package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** Availability of a voice reference after resolving library, samples and engine readiness. */
public enum VoiceReferenceAvailability {
    AVAILABLE,
    BUILT_IN,
    MISSING_PROFILE,
    MISSING_SAMPLE,
    ENGINE_UNAVAILABLE,
    UNSUPPORTED_FOR_MODE;

    public boolean usable() {
        return this == AVAILABLE || this == BUILT_IN;
    }
}
