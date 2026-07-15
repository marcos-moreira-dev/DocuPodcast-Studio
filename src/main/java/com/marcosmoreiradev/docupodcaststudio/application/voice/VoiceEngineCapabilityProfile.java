package com.marcosmoreiradev.docupodcaststudio.application.voice;

/**
 * User-facing capability profile for the currently selected voice engine.
 *
 * <p>This is the single contract used by Documento, Voces and Configuración to avoid
 * showing controls that the active engine cannot honor. The advanced AI voice is the expressive
 * high-quality path, the simple local voice is the lightweight narrator, and test mode is diagnostic only.</p>
 */
public record VoiceEngineCapabilityProfile(
        String engineFamily,
        String displayName,
        String sidebarNotice,
        boolean coquiXttsMode,
        boolean piperMode,
        boolean mockMode,
        boolean supportsCustomVoiceSample,
        boolean supportsPiperModelVoice,
        boolean supportsEmotion,
        boolean supportsExpressiveStyle,
        boolean supportsVoiceCloning,
        boolean supportsMultipleImportedVoices,
        boolean canSynthesizeNow,
        String blockedReason
) {
    public VoiceEngineCapabilityProfile {
        engineFamily = normalize(engineFamily).isBlank() ? "unknown" : normalize(engineFamily);
        displayName = normalize(displayName).isBlank() ? engineFamily : normalize(displayName);
        sidebarNotice = normalize(sidebarNotice);
        blockedReason = normalize(blockedReason);
    }

    public static VoiceEngineCapabilityProfile coquiXtts(boolean ready, String label) {
        return new VoiceEngineCapabilityProfile(
                "coqui-xtts",
                normalize(label).isBlank() ? "Voz IA avanzada" : friendlyAdvancedLabel(label),
                "Voz IA avanzada activa. Puedes usar voz neutral, voces importadas por muestra y opciones expresivas cuando el motor las soporte.",
                true,
                false,
                false,
                true,
                false,
                true,
                true,
                true,
                true,
                ready,
                ready ? "" : "Prepara Voz IA avanzada para generar voz real con muestras y opciones expresivas."
        );
    }

    public static VoiceEngineCapabilityProfile piper(boolean ready, String label) {
        return new VoiceEngineCapabilityProfile(
                "piper",
                normalize(label).isBlank() ? "Voz local simple" : friendlyLocalSimpleLabel(label),
                "Voz local simple activa. Este modo ofrece lectura local intermedia. Las voces personalizadas por muestra, emociones y estilos expresivos requieren Voz IA avanzada.",
                false,
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                ready,
                ready ? "" : "Verifica Voz local simple y una voz .onnx con su .onnx.json antes de generar voz real."
        );
    }

    public static VoiceEngineCapabilityProfile mock() {
        return new VoiceEngineCapabilityProfile(
                "mock",
                "Modo de prueba",
                "Modo de prueba activo: no genera voz real. Configura Voz IA avanzada o Voz local simple para escuchar documentos con síntesis real.",
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "El motor mock solo valida flujo, jobs y exportación técnica."
        );
    }

    public static VoiceEngineCapabilityProfile localProcess(boolean ready, String label) {
        return new VoiceEngineCapabilityProfile(
                "local-process",
                normalize(label).isBlank() ? "Motor TTS local" : normalize(label),
                "Motor TTS local activo. Solo se muestran controles seguros; las capacidades avanzadas dependen del motor configurado.",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                ready,
                ready ? "" : "Verifica el motor TTS local antes de generar audio real."
        );
    }

    public boolean supportsAdvancedExpressiveControls() {
        return supportsEmotion || supportsExpressiveStyle || supportsVoiceCloning;
    }

    private static String friendlyAdvancedLabel(String value) {
        String normalized = normalize(value);
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("coqui") || lower.contains("xtts")) {
            return "Voz IA avanzada";
        }
        return normalized;
    }

    private static String friendlyLocalSimpleLabel(String value) {
        String normalized = normalize(value);
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("piper")) {
            return "Voz local simple";
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
