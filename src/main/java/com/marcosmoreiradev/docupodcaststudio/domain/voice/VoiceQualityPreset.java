package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** User-facing quality/speed intent for a voice profile. */
public enum VoiceQualityPreset {
    FAST("Rápida"),
    BALANCED("Equilibrada"),
    HIGH_QUALITY("Alta calidad"),
    HUMAN_REFERENCE("Referencia humana");

    private final String displayName;

    VoiceQualityPreset(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
