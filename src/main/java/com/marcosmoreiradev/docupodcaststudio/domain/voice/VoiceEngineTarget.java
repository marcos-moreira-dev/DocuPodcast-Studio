package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** User-facing engine grouping for the voice library. Technical engine names stay outside the UX. */
public enum VoiceEngineTarget {
    ADVANCED_AI_VOICE("Voz IA avanzada"),
    LOCAL_SIMPLE_VOICE("Voz local simple"),
    TEST_MODE("Modo de prueba");

    private final String displayName;

    VoiceEngineTarget(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean supportsReferenceSamples() {
        return this == ADVANCED_AI_VOICE;
    }

    public boolean isMinimalMode() {
        return this == LOCAL_SIMPLE_VOICE || this == TEST_MODE;
    }
}
