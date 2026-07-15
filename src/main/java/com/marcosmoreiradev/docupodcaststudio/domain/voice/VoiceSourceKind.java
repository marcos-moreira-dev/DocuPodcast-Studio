package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** Source selected for reading a text range or segment. */
public enum VoiceSourceKind {
    AI_TTS("Voz IA"),
    HUMAN_RECORDING("Audio del computador"),
    UNASSIGNED("Sin asignar");

    private final String displayName;

    VoiceSourceKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
