package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.compatibility.voice.LegacyVoiceProfileEngineCompatibility;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;

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
        boolean advancedAiMode,
        boolean simpleLocalMode,
        boolean diagnosticMode,
        boolean supportsCustomVoiceSample,
        boolean supportsPackagedModelVoice,
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

    public static VoiceEngineCapabilityProfile advancedAi(boolean ready, String label) {
        return new VoiceEngineCapabilityProfile(
                "advanced-ai",
                normalize(label).isBlank() ? "Voz IA avanzada" : normalize(label),
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

    public static VoiceEngineCapabilityProfile simpleLocal(boolean ready, String label) {
        return new VoiceEngineCapabilityProfile(
                "simple-local",
                normalize(label).isBlank() ? "Voz local simple" : normalize(label),
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

    public static VoiceEngineCapabilityProfile diagnostic() {
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

    public static VoiceEngineCapabilityProfile capabilityDriven(
            boolean ready, String label, boolean referenceVoice,
            boolean expressiveStyle, boolean packagedVoice) {
        boolean advanced = referenceVoice || expressiveStyle;
        if (!advanced && !packagedVoice) {
            return localProcess(ready, label);
        }
        String display = normalize(label).isBlank() ? "Motor TTS local" : normalize(label);
        String notice = advanced
                ? "Motor de voz avanzado activo. Los controles visibles se derivan de sus capacidades declaradas."
                : "Motor de voz local activo. No declara clonación por muestra ni estilo expresivo.";
        return new VoiceEngineCapabilityProfile(
                advanced ? "advanced-local" : "simple-local",
                display,
                notice,
                advanced,
                !advanced,
                false,
                referenceVoice,
                packagedVoice,
                expressiveStyle,
                expressiveStyle,
                referenceVoice,
                referenceVoice,
                ready,
                ready ? "" : "Prepara el motor local seleccionado desde Configuración antes de sintetizar."
        );
    }

    public boolean supportsAdvancedExpressiveControls() {
        return supportsEmotion || supportsExpressiveStyle || supportsVoiceCloning;
    }

    public boolean supportsVoiceProfile(VoiceProfile voice) {
        return LegacyVoiceProfileEngineCompatibility.supports(
                voice, advancedAiMode, simpleLocalMode, diagnosticMode);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
