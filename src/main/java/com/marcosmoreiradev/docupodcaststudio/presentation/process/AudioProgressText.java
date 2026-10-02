package com.marcosmoreiradev.docupodcaststudio.presentation.process;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;

/** Shared wording for standalone voice generation and the audio phase of an export. */
public final class AudioProgressText {
    private AudioProgressText() {}

    public static String title(AudioJobStatusDto status) {
        return switch (status.stage()) {
            case NONE, PREPARING_WORKSPACE -> "Preparando el audio";
            case WAITING_FOR_RESOURCES -> "Esperando recursos para generar audio";
            case GENERATING_SEGMENTS -> "Generando y preparando fragmentos de voz";
            case MERGING_SEGMENTS -> "Uniendo fragmentos de audio";
            case EXPORT_READY -> "Audio listo";
            case CANCELLED -> "Generación de audio cancelada";
            case FAILED -> "No se pudo completar el audio";
        };
    }

    public static String timing(AudioJobStatusDto status) {
        return switch (status.stage()) {
            case GENERATING_SEGMENTS -> "Generación de voz: " + status.etaLabel();
            case WAITING_FOR_RESOURCES -> "Tiempo restante no disponible mientras se esperan recursos.";
            case MERGING_SEGMENTS -> "Uniendo los audios generados; tiempo restante aún no disponible.";
            case EXPORT_READY -> "Fragmentos de audio preparados.";
            case CANCELLED -> "Se conservarán los audios válidos.";
            case FAILED -> "Consulta el detalle del error.";
            default -> "Preparando la generación; calculando tiempo restante…";
        };
    }
}
