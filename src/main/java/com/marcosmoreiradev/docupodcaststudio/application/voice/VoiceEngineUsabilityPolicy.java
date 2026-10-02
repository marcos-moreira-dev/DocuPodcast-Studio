package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Product-level voice engine catalog and capability policy.
 *
 * <p>Product callers project the active engine registry. Provider-specific factories remain only
 * for format-v1 compatibility and tests; raw command lines stay out of the normal workflow.</p>
 */
public final class VoiceEngineUsabilityPolicy {
    private VoiceEngineUsabilityPolicy() {
    }

    /** Product projection: the executable registry is the authority, not this compatibility catalog. */
    public static List<VoiceEngineOption> registeredOptions(MediaEnginePlatform platform) {
        MediaEnginePlatform active = platform == null ? MediaEnginePlatform.empty() : platform;
        return active.voiceEngines().descriptors().stream()
                .map(VoiceEngineUsabilityPolicy::fromDescriptor)
                .toList();
    }

    private static VoiceEngineOption fromDescriptor(EngineDescriptor descriptor) {
        EnumSet<VoiceEngineControl> controls = EnumSet.of(VoiceEngineControl.VOLUME);
        if (!descriptor.diagnosticOnly()) {
            controls.add(VoiceEngineControl.SPEED);
            controls.add(VoiceEngineControl.LANGUAGE);
            controls.add(VoiceEngineControl.DEVICE);
            controls.add(VoiceEngineControl.MODEL_FOLDER);
        }
        if (descriptor.supports(EngineFeature.REFERENCE_VOICE)) {
            controls.add(VoiceEngineControl.REFERENCE_VOICE);
        }
        if (descriptor.supports(EngineFeature.EXPRESSIVE_STYLE)) {
            controls.add(VoiceEngineControl.EMOTION_INTENT);
        }
        return new VoiceEngineOption(descriptor.id().value(), descriptor.displayName(),
                descriptor.runtimeKind(), descriptor.diagnosticOnly()
                ? "Diagnóstico del flujo sin voz final."
                : descriptor.supports(EngineFeature.REFERENCE_VOICE)
                ? "Narración local con personajes y referencias autorizadas."
                : "Lectura local sin clonación por muestra.",
                "Administrado desde Configuración", "piper".equals(descriptor.id().value()),
                !descriptor.diagnosticOnly(), descriptor.diagnosticOnly(), controls);
    }

    /** @deprecated compatibility catalog for format-v1 tests and migrations. */
    @Deprecated(forRemoval = false)
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

    /** @deprecated use {@link #registeredOptions(MediaEnginePlatform)}. */
    @Deprecated(forRemoval = false)
    public static List<VoiceEngineOption> recommendedOptions() {
        return List.of(xttsHighQuality(), piperLightweight(), mockDiagnostic());
    }

    public static VoiceEngineOption defaultEngine() {
        return piperLightweight();
    }

    public static Optional<VoiceEngineOption> find(String id) {
        String target = id == null ? "" : id.strip();
        return recommendedOptions().stream().filter(option -> option.id().equals(target)).findFirst();
    }

    public static boolean shouldExposeRawCommandToNormalUser() {
        return false;
    }
}
