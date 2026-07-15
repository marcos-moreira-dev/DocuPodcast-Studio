package com.marcosmoreiradev.docupodcaststudio.application.engines;

/** Product-level purpose of an external engine in DocuPodcast Studio. */
public enum AiEnginePurpose {
    TTS_HIGH_QUALITY("Texto a voz de calidad alta"),
    TTS_LIGHTWEIGHT("Texto a voz liviano"),
    OCR_TEXT_EXTRACTION("OCR local para PDF"),
    MEDIA_AUDIO_EXTRACTION("Extracción de audio desde video"),
    VIDEO_RENDER("Render de video simple");

    private final String label;

    AiEnginePurpose(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
