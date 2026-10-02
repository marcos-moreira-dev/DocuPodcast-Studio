package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.VoiceSynthesisEngine;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Comparator;

/** Lists every voice engine from the same executable registry used for synthesis. */
public final class ListVoiceEngineOperationalStatesUseCase {
    public static final String PIPER_ID = "piper";
    public static final String XTTS_ID = "xtts";

    private final MediaEnginePlatform platform;

    public ListVoiceEngineOperationalStatesUseCase(MediaEnginePlatform platform) {
        this.platform = Objects.requireNonNullElseGet(platform, MediaEnginePlatform::empty);
    }

    public List<VoiceEngineOperationalState> list() {
        return platform.voiceEngines().engines().stream()
                .sorted(Comparator.comparingInt(ListVoiceEngineOperationalStatesUseCase::productOrder)
                        .thenComparing(engine -> engine.descriptor().displayName(),
                                String.CASE_INSENSITIVE_ORDER))
                .map(ListVoiceEngineOperationalStatesUseCase::state)
                .toList();
    }

    public VoiceEngineOperationalState find(String engineId) {
        String target = engineId == null ? "" : engineId.strip().toLowerCase(Locale.ROOT);
        return list().stream().filter(state -> state.engineId().equals(target)).findFirst()
                .orElseGet(() -> missing(target, target, "Motor no reconocido."));
    }

    private static VoiceEngineOperationalState state(VoiceSynthesisEngine engine) {
        EngineReadiness readiness;
        try {
            readiness = engine.inspectReadiness(null);
        } catch (RuntimeException failure) {
            readiness = EngineReadiness.unavailable(engine.descriptor().id(),
                    "No se pudo comprobar " + engine.descriptor().displayName() + ".",
                    Objects.toString(failure.getMessage(), "Revisa Motores y dependencias."));
        }
        return new VoiceEngineOperationalState(engine.descriptor().id().value(),
                engine.descriptor().displayName(), true,
                readiness.ready(), !engine.descriptor().diagnosticOnly(),
                engine.descriptor().supports(EngineFeature.REFERENCE_VOICE),
                engine.descriptor().supports(EngineFeature.EXPRESSIVE_STYLE),
                readiness.ready() ? "Listo para la lectura" : "Requiere reparación",
                readiness.summary(), readiness.issues(), readiness.recommendedActions());
    }

    private static int productOrder(VoiceSynthesisEngine engine) {
        String id = engine.descriptor().id().value();
        if (PIPER_ID.equals(id)) return 0;
        if (XTTS_ID.equals(id)) return 1;
        if ("qwen3-tts-local".equals(id)) return 2;
        return engine.descriptor().diagnosticOnly() ? 100 : 50;
    }

    private static VoiceEngineOperationalState missing(String id, String name, String message) {
        return new VoiceEngineOperationalState(id, name, false, false, true,
                false, false, "Motor no registrado", message,
                List.of("No se registró el motor " + id + "."),
                List.of("Abre DocuPodcast Studio mediante el lanzador productivo."));
    }
}
