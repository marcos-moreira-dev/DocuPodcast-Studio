package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Product-level voice engine catalog and capability policy.
 *
 * <p>The policy keeps the main reader friendly: the advanced AI voice is the preferred high-quality path, the simple
 * local voice is the intermediate/lightweight option, and test mode remains diagnostic. Raw command lines stay out of the
 * normal user workflow.</p>
 */
public final class VoiceEngineUsabilityPolicy {
    private VoiceEngineUsabilityPolicy() {
    }

    public static VoiceEngineOption xttsHighQuality() {
        return new VoiceEngineOption(
                "tts-xtts",
                "Voz IA avanzada — calidad alta",
                "Motor avanzado prioritario",
                "Narración con voz más humana, personajes y voces autorizadas de referencia.",
                "models/tts/xtts",
                true,
                true,
                false,
                EnumSet.of(
                        VoiceEngineControl.SPEED,
                        VoiceEngineControl.VOLUME,
                        VoiceEngineControl.REFERENCE_VOICE,
                        VoiceEngineControl.EMOTION_INTENT,
                        VoiceEngineControl.LANGUAGE,
                        VoiceEngineControl.DEVICE,
                        VoiceEngineControl.MODEL_FOLDER
                )
        );
    }

    public static VoiceEngineOption piperLightweight() {
        return new VoiceEngineOption(
                "tts-piper",
                "Voz local simple — liviana",
                "Motor local simple",
                "Lectura local simple y estable en equipos modestos cuando la Voz IA avanzada sea demasiado pesada.",
                "models/tts/piper/voices",
                false,
                true,
                false,
                EnumSet.of(
                        VoiceEngineControl.SPEED,
                        VoiceEngineControl.VOLUME,
                        VoiceEngineControl.LANGUAGE,
                        VoiceEngineControl.MODEL_FOLDER
                )
        );
    }

    public static VoiceEngineOption mockDiagnostic() {
        return new VoiceEngineOption(
                "tts-mock",
                "Modo de prueba integrado",
                "Diagnóstico",
                "Genera WAVs válidos para probar flujo, jobs y exportación sin voz real.",
                "sin modelo externo",
                false,
                false,
                true,
                EnumSet.of(VoiceEngineControl.VOLUME)
        );
    }

    public static List<VoiceEngineOption> recommendedOptions() {
        return List.of(xttsHighQuality(), piperLightweight(), mockDiagnostic());
    }

    public static VoiceEngineOption defaultEngine() {
        return xttsHighQuality();
    }

    public static Optional<VoiceEngineOption> find(String id) {
        String target = id == null ? "" : id.strip();
        return recommendedOptions().stream().filter(option -> option.id().equals(target)).findFirst();
    }

    public static boolean shouldExposeRawCommandToNormalUser() {
        return false;
    }
}
