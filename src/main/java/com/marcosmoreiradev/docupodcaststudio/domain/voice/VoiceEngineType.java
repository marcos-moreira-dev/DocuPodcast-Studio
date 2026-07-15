package com.marcosmoreiradev.docupodcaststudio.domain.voice;

/** Engine family that can synthesize or provide a voice. */
public enum VoiceEngineType {
    MOCK("Modo de prueba"),
    LOCAL_TTS_PROCESS("Motor local por proceso"),
    XTTS("Voz IA avanzada"),
    PIPER("Voz local simple"),
    HUMAN_AUDIO("Audio humano"),
    UNKNOWN("Desconocido");

    private final String displayName;

    VoiceEngineType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
