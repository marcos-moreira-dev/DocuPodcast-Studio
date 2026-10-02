package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.PerformanceStyle;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;

import java.util.List;

/**
 * Evaluates which voice and style promises are real with the current audio engine.
 *
 * <p>The policy keeps the UI honest: a profile may be assignable to a segment
 * while not yet being synthesizable by the configured engine.</p>
 */
public final class VoiceCapabilityPolicy {

    public VoiceEngineCapabilityProfile activeEngineProfile(AudioEngineDescriptor engine) {
        if (engine == null || engine.diagnosticOnly() || !engine.realTts()) {
            return VoiceEngineCapabilityProfile.diagnostic();
        }
        return VoiceEngineCapabilityProfile.capabilityDriven(
                engine.configured(), engine.displayName(),
                engine.supports(EngineFeature.REFERENCE_VOICE),
                engine.supports(EngineFeature.EXPRESSIVE_STYLE),
                engine.supports(EngineFeature.PACKAGED_VOICE));
    }

    public boolean supportsCustomVoiceSample(AudioEngineDescriptor engine) {
        return activeEngineProfile(engine).supportsCustomVoiceSample();
    }

    public boolean supportsPackagedModelVoice(AudioEngineDescriptor engine) {
        return activeEngineProfile(engine).supportsPackagedModelVoice();
    }

    public boolean supportsEmotion(AudioEngineDescriptor engine) {
        return activeEngineProfile(engine).supportsEmotion();
    }

    public boolean supportsExpressiveStyle(AudioEngineDescriptor engine) {
        return activeEngineProfile(engine).supportsExpressiveStyle();
    }

    public boolean supportsVoiceCloning(AudioEngineDescriptor engine) {
        return activeEngineProfile(engine).supportsVoiceCloning();
    }

    public boolean supportsMultipleImportedVoices(AudioEngineDescriptor engine) {
        return activeEngineProfile(engine).supportsMultipleImportedVoices();
    }

    public boolean canSynthesizeNow(AudioEngineDescriptor engine) {
        return activeEngineProfile(engine).canSynthesizeNow();
    }
    public VoiceLibraryCapabilityReport evaluate(VoiceLibrary library, AudioEngineDescriptor engine) {
        if (library == null) {
            return VoiceLibraryCapabilityReport.empty(engineLabel(engine));
        }
        List<VoiceProfileCapability> voiceCapabilities = library.voices().stream()
                .map(voice -> evaluateVoice(voice, engine))
                .toList();
        List<PerformanceStyleCapability> styleCapabilities = library.styles().stream()
                .map(style -> evaluateStyle(style, engine))
                .toList();
        int assignable = (int) voiceCapabilities.stream().filter(VoiceProfileCapability::assignable).count();
        int synthesizable = (int) voiceCapabilities.stream().filter(VoiceProfileCapability::synthesizableNow).count();
        int reference = (int) voiceCapabilities.stream().filter(VoiceProfileCapability::referenceReady).count();
        int blocked = (int) voiceCapabilities.stream().filter(VoiceProfileCapability::blocked).count();
        int honored = (int) styleCapabilities.stream().filter(PerformanceStyleCapability::honoredByCurrentEngine).count();
        int roadmap = (int) styleCapabilities.stream().filter(PerformanceStyleCapability::roadmapOnly).count();
        return new VoiceLibraryCapabilityReport(engineLabel(engine), voiceCapabilities, styleCapabilities, assignable, synthesizable, reference, blocked, honored, roadmap);
    }

    public VoiceProfileCapability evaluateVoice(VoiceProfile voice, AudioEngineDescriptor engine) {
        if (voice == null) {
            return new VoiceProfileCapability("", "", false, false, false, true, false, false,
                    "Inválida", "Perfil de voz nulo.");
        }
        VoiceEngineCapabilityProfile profile = activeEngineProfile(engine);
        boolean engineConfigured = engine != null && engine.configured();
        boolean realTts = engine != null && engine.realTts();
        boolean mock = profile.diagnosticMode();
        boolean assignable = true;
        boolean referenceVoice = requiresReferenceVoice(voice);

        if (profile.simpleLocalMode() && referenceVoice) {
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, voice.hasSample() || voice.hasModel(), false, true, false,
                    "Motor sin referencias", "El motor activo no puede representar esta voz/personaje. Elige Coqui XTTS o Qwen3-TTS; la voz y su emoción se conservarán.");
        }

        if (legacyDefaultAdvancedReference(voice)) {
            boolean synth = profile.supportsVoiceCloning() && engineConfigured;
            return new VoiceProfileCapability(voice.id(), "Narrador prediseñado avanzado", assignable, synth, true, false, !synth, !synth,
                    synth ? "Neutral prediseñada" : "Requiere Voz IA avanzada",
                    synth ? "Voz IA avanzada puede usar la referencia Neutral prediseñada."
                            : "Esta referencia Neutral se usa cuando Voz IA avanzada está activa.");
        }

        if (OfficialAdvancedVoicePresetCatalog.isOfficialPreset(voice)) {
            boolean synth = profile.supportsVoiceCloning() && engineConfigured;
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, synth, true, false, !synth, !synth,
                    synth ? "Voz IA avanzada lista" : "Requiere Voz IA avanzada",
                    synth ? "Voz IA avanzada puede usar las muestras oficiales de esta voz."
                            : "Esta voz oficial se usa cuando Voz IA avanzada está activa.");
        }

        if (mock && "VOC-NARRATOR".equals(voice.id()) && "true".equalsIgnoreCase(voice.metadata().get("builtIn"))) {
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, true, false, false,
                    false, false, "Modo de prueba listo",
                    "Disponible únicamente cuando el modo diagnóstico mock fue seleccionado explícitamente.");
        }

        if (profile.supportsVoiceCloning() && voice.engineType() == VoiceEngineType.HUMAN_AUDIO) {
            if (!voice.hasSample()) {
                return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, false, true, false, true,
                        "Requiere muestra", "Voz IA avanzada puede usar voces importadas cuando exista una muestra registrada.");
            }
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, engineConfigured, true, false, !engineConfigured, true,
                    engineConfigured ? "Muestra lista" : "Motor pendiente", engineConfigured
                    ? "Voz IA avanzada puede usar esta muestra como referencia de voz."
                    : "Prepara Voz IA avanzada antes de sintetizar esta voz importada.");
        }

        if (voice.engineType() == VoiceEngineType.HUMAN_AUDIO) {
            if (!voice.hasSample()) {
                return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, false, true, false, false,
                        "Requiere muestra", "Puede asignarse como intención, pero necesita una muestra antes de usarse como voz humana.");
            }
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, true, false, false, true,
                    "Muestra lista", "La muestra humana está registrada como referencia. La clonación/síntesis con muestra depende de un motor avanzado posterior.");
        }

        if (voice.engineType() == VoiceEngineType.MOCK) {
            if (mock) {
                return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, true, false, false, false, false,
                        "Modo de prueba listo", "Genera audio de prueba para validar el flujo, sin voz real.");
            }
            if (realTts && engineConfigured) {
                return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, true, false, false, false, false,
                        "TTS por defecto", "Usable con el motor TTS configurado como voz por defecto del comando externo.");
            }
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, false, false, true, false,
                    "Motor no configurado", "Configura un motor de voz o usa el modo de prueba para validar esta voz.");
        }

        if (voice.engineType() == VoiceEngineType.LOCAL_TTS_PROCESS || voice.engineType() == VoiceEngineType.PIPER) {
            boolean builtInNarrator = "VOC-NARRATOR".equals(voice.id())
                    && "true".equalsIgnoreCase(voice.metadata().get("builtIn"));
            if (realTts && engineConfigured) {
                return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, true, voice.hasSample(), false, false, false,
                        builtInNarrator ? "TTS por defecto" : "TTS listo",
                        "Compatible con el motor local configurado. La voz exacta dependerá del comando y sus parámetros.");
            }
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, voice.hasSample(), false, true, false,
                    builtInNarrator ? "Motor no configurado" : "Motor pendiente",
                    "Requiere motor TTS local configurado antes de sintetizar.");
        }

        if (referenceVoice) {
            boolean hasReference = voice.hasSample() || voice.hasModel();
            boolean synth = profile.supportsVoiceCloning() && engineConfigured && hasReference;
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, synth, hasReference, !hasReference, !synth, true,
                    synth ? "Motor con referencias listo" : "Requiere motor con referencias",
                    synth ? "Perfil compatible con " + profile.displayName() + "."
                            : "Prepara Coqui XTTS o Qwen3-TTS y una muestra antes de sintetizar esta voz.");
        }

        if (voice.type() == VoiceProfileType.AUTHORIZED || voice.type() == VoiceProfileType.IMPORTED) {
            return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, voice.hasSample() || voice.hasModel(), !voice.hasSample() && !voice.hasModel(), false, true,
                    "Autorización requerida", "Debe conservar muestra/modelo y nota de consentimiento antes de uso productivo.");
        }

        return new VoiceProfileCapability(voice.id(), voice.displayName(), assignable, false, false, false, true, true,
                "Sin motor compatible", "Perfil asignable, pero no hay motor compatible declarado para sintetizarlo ahora.");
    }

    public PerformanceStyleCapability evaluateStyle(PerformanceStyle style, AudioEngineDescriptor engine) {
        if (style == null) {
            return new PerformanceStyleCapability("", "", false, false, false, false,
                    "Inválido", "Estilo nulo.");
        }
        VoiceEngineCapabilityProfile profile = activeEngineProfile(engine);
        boolean realTts = engine != null && engine.realTts() && engine.configured();
        if (!style.requiresEngineSupport()) {
            return new PerformanceStyleCapability(style.id(), style.displayName(), true, true, false, false,
                    "Básico", "Se puede asignar como estilo base en cualquier motor.");
        }
        if (profile.simpleLocalMode()) {
            return new PerformanceStyleCapability(style.id(), style.displayName(), false, false, false, true,
                    "No disponible con Voz local simple", "Voz local simple ofrece lectura local intermedia; las emociones y estilos expresivos requieren Voz IA avanzada.");
        }
        if (profile.supportsExpressiveStyle() && realTts) {
            return new PerformanceStyleCapability(style.id(), style.displayName(), true, true, false, false,
                    "Voz IA avanzada", "Disponible como intención expresiva cuando el motor avanzado activo lo soporte.");
        }
        if (realTts) {
            return new PerformanceStyleCapability(style.id(), style.displayName(), true, false, true, true,
                    "Intención", "Se guarda como intención. El motor actual puede ignorarla si no soporta estilos/emoción.");
        }
        return new PerformanceStyleCapability(style.id(), style.displayName(), false, false, true, true,
                "No disponible", "Requiere motor con soporte expresivo real.");
    }

    private static String engineLabel(AudioEngineDescriptor engine) {
        return engine == null ? "Motor de audio no disponible" : engine.statusLabel();
    }

    private static boolean legacyDefaultAdvancedReference(VoiceProfile voice) {
        return voice != null && "VOC-OWN-PLACEHOLDER".equalsIgnoreCase(voice.id())
                && !voice.metadata().containsKey("userManaged");
    }

    private static boolean requiresReferenceVoice(VoiceProfile voice) {
        if (voice == null) return false;
        if (OfficialAdvancedVoicePresetCatalog.isOfficialPreset(voice)) return true;
        if (voice.engineType() == VoiceEngineType.XTTS
                || voice.engineType() == VoiceEngineType.HUMAN_AUDIO) return true;
        return voice.supportsStyleTransfer()
                || (voice.hasSample()
                && voice.engineType() != VoiceEngineType.PIPER
                && voice.engineType() != VoiceEngineType.LOCAL_TTS_PROCESS);
    }
}
