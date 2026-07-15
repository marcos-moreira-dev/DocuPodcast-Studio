package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Objects;

/**
 * Product-level plan for the single visible action "Escuchar documento".
 *
 * <p>The UI should not expose the machinery as the main path. This plan keeps the end-to-end
 * listening brain explicit: open source → narrated document → internal narration projection → audio
 * buffer/playback.</p>
 */
public record DocumentListenPlan(
        DocumentListenPhase phase,
        String userMessage,
        boolean keepReaderVisible,
        boolean requiresNarrationProjection,
        boolean requiresAudioGeneration,
        boolean canPlayImmediately
) {
    public DocumentListenPlan {
        Objects.requireNonNull(phase, "phase");
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static DocumentListenPlan noDocument() {
        return new DocumentListenPlan(
                DocumentListenPhase.NO_DOCUMENT,
                "Abre un documento antes de escuchar.",
                true,
                false,
                false,
                false);
    }

    public static DocumentListenPlan noNarratableText() {
        return new DocumentListenPlan(
                DocumentListenPhase.NO_NARRATABLE_TEXT,
                "El documento no tiene texto narrable con el perfil actual.",
                true,
                false,
                false,
                false);
    }

    public static DocumentListenPlan buildNarrationProjection() {
        return new DocumentListenPlan(
                DocumentListenPhase.BUILD_NARRATION_PROJECTION,
                "Preparando la narración interna del documento.",
                true,
                true,
                false,
                false);
    }

    public static DocumentListenPlan playExistingAudio() {
        return new DocumentListenPlan(
                DocumentListenPhase.PLAY_EXISTING_AUDIO,
                "Audio disponible. Reproduciendo desde el documento.",
                true,
                false,
                false,
                true);
    }

    public static DocumentListenPlan waitForAudioBuffer(String bufferLabel) {
        String suffix = bufferLabel == null || bufferLabel.isBlank() ? "" : " " + bufferLabel.strip();
        return new DocumentListenPlan(
                DocumentListenPhase.WAIT_FOR_AUDIO_BUFFER,
                "Preparando audio por fragmentos." + suffix,
                true,
                false,
                false,
                false);
    }

    public static DocumentListenPlan saveProjectRequired() {
        return new DocumentListenPlan(
                DocumentListenPhase.SAVE_PROJECT_REQUIRED,
                "Guarda el proyecto antes de generar audio por segmentos.",
                true,
                false,
                false,
                false);
    }

    public static DocumentListenPlan generateAudio() {
        return new DocumentListenPlan(
                DocumentListenPhase.GENERATE_AUDIO,
                "Generando audio por segmentos para iniciar lectura con buffer.",
                true,
                false,
                true,
                false);
    }
}
