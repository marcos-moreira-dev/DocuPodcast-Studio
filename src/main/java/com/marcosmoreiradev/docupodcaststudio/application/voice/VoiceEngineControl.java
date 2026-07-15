package com.marcosmoreiradev.docupodcaststudio.application.voice;

/** User-facing controls that a voice engine may expose through guided settings. */
public enum VoiceEngineControl {
    SPEED("Velocidad"),
    VOLUME("Volumen"),
    PITCH("Tono"),
    REFERENCE_VOICE("Voz de referencia"),
    EMOTION_INTENT("Intención/emoción"),
    LANGUAGE("Idioma"),
    DEVICE("CPU/GPU"),
    MODEL_FOLDER("Carpeta de modelos");

    private final String label;

    VoiceEngineControl(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
