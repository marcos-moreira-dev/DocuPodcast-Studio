package com.marcosmoreiradev.docupodcaststudio.application.settings;

/** Internal identifiers and safe labels for TTS engine modes. */
public final class TtsEngineModes {
    public static final String TEST = "mock";
    public static final String EXTERNAL = "external";
    public static final String LOCAL_SIMPLE = "piper";
    public static final String ADVANCED_AI = "xtts";

    private TtsEngineModes() {
    }

    public static String safeLabel(String mode) {
        String normalized = mode == null ? "" : mode.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case LOCAL_SIMPLE -> "Voz local simple";
            case ADVANCED_AI -> "Voz IA avanzada";
            case EXTERNAL -> "Comando externo avanzado";
            case TEST -> "Modo de prueba";
            default -> mode == null || mode.isBlank() ? "Modo de prueba" : mode.strip();
        };
    }

    public static String backendLabel(String mode) {
        String normalized = mode == null ? "" : mode.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case LOCAL_SIMPLE -> "Piper";
            case ADVANCED_AI -> "Coqui XTTS";
            case TEST -> "Diagnóstico de audio";
            default -> "";
        };
    }
}
