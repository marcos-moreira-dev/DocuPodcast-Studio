package com.marcosmoreiradev.docupodcaststudio.application.audio;

/**
 * Operational availability of a voice/audio source as seen by Documento.
 *
 * <p>This is not a decorative catalog. It answers one product question: can the user choose this
 * origin for the selected document fragment right now? Configuration screens may show unavailable
 * engines with a repair action; Documento must only expose usable options plus Audio del computador.</p>
 */
public record AudioEngineAvailability(
        String engineId,
        String displayName,
        String mode,
        boolean usableInDocument,
        boolean realTts,
        boolean supportsVoiceSamples,
        boolean supportsTones,
        String statusLabel,
        String userMessage,
        String recommendedAction
) {
    public AudioEngineAvailability {
        engineId = normalize(engineId).isBlank() ? "unknown" : normalize(engineId);
        displayName = normalize(displayName).isBlank() ? engineId : normalize(displayName);
        mode = normalize(mode).isBlank() ? engineId : normalize(mode);
        statusLabel = normalize(statusLabel);
        userMessage = normalize(userMessage);
        recommendedAction = normalize(recommendedAction);
    }

    public static AudioEngineAvailability advanced(boolean usable, String message, String action) {
        return new AudioEngineAvailability(
                "xtts",
                "Voz IA avanzada",
                "xtts",
                usable,
                true,
                true,
                true,
                usable ? "Lista para Documento" : "Requiere reparación",
                message,
                action);
    }

    public static AudioEngineAvailability localSimple(boolean usable, String message, String action) {
        return new AudioEngineAvailability(
                "piper",
                "Voz local simple",
                "piper",
                usable,
                true,
                false,
                false,
                usable ? "Lista para Documento" : "Requiere preparación",
                message,
                action);
    }

    public static AudioEngineAvailability testMode() {
        return new AudioEngineAvailability(
                "mock",
                "Modo de prueba",
                "mock",
                true,
                false,
                false,
                false,
                "Siempre disponible",
                "Valida el flujo sin generar voz real.",
                "Usa un motor real para escuchar documentos con voz final.");
    }

    public static AudioEngineAvailability computerAudio() {
        return new AudioEngineAvailability(
                "computer-audio",
                "Audio del computador",
                "file",
                true,
                false,
                false,
                false,
                "Disponible por fragmento",
                "Permite elegir un audio existente o extraer audio de un video para la selección.",
                "Selecciona una oración y elige un archivo compatible.");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
