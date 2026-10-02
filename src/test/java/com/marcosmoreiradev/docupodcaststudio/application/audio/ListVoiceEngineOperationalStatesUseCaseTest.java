package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfigurationSchema;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.VoiceSynthesisEngine;
import com.marcosmoreiradev.docupodcaststudio.media.api.VoiceSynthesisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.VoiceSynthesisResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ListVoiceEngineOperationalStatesUseCaseTest {
    @Test
    void exposesEveryRegisteredEngineInProductOrderUsingRealReadiness() {
        EngineRegistry<VoiceSynthesisEngine> voices = new EngineRegistry<>(CapabilityId.VOICE_SYNTHESIS);
        voices.register(new FixedVoiceEngine("piper", "Voz local simple",
                EngineReadiness.ready(new EngineId("piper"), "Piper listo"), Set.of()));
        voices.register(new FixedVoiceEngine("xtts", "Voz IA avanzada",
                EngineReadiness.unavailable(new EngineId("xtts"), "Falta una muestra", "Importa una muestra"),
                Set.of(EngineFeature.REFERENCE_VOICE, EngineFeature.EXPRESSIVE_STYLE)));
        voices.register(new FixedVoiceEngine("qwen3-tts-local", "Qwen3-TTS local · 1.7B Q8",
                EngineReadiness.ready(new EngineId("qwen3-tts-local"), "Qwen listo"),
                Set.of(EngineFeature.REFERENCE_VOICE, EngineFeature.EXPRESSIVE_STYLE)));

        List<VoiceEngineOperationalState> states = new ListVoiceEngineOperationalStatesUseCase(
                new MediaEnginePlatform(voices, null, null)).list();

        assertEquals(List.of("piper", "xtts", "qwen3-tts-local"),
                states.stream().map(VoiceEngineOperationalState::engineId).toList());
        assertTrue(states.getFirst().registered());
        assertTrue(states.getFirst().ready());
        assertFalse(states.getFirst().supportsVoiceSamples());
        assertTrue(states.get(1).registered());
        assertFalse(states.get(1).ready());
        assertTrue(states.get(1).supportsVoiceSamples());
        assertTrue(states.get(1).supportsTones());
        assertTrue(states.get(1).message().contains("Falta una muestra"));
        assertTrue(states.get(1).recommendedAction().contains("Importa una muestra"));
        assertTrue(states.get(2).supportsVoiceSamples());
    }

    @Test
    void emptyRegistryDoesNotInventProviderSpecificEngines() {
        List<VoiceEngineOperationalState> states =
                new ListVoiceEngineOperationalStatesUseCase(MediaEnginePlatform.empty()).list();

        assertTrue(states.isEmpty());
    }

    private record FixedVoiceEngine(
            String id,
            String name,
            EngineReadiness readiness,
            Set<EngineFeature> features
    ) implements VoiceSynthesisEngine {
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId(id), CapabilityId.VOICE_SYNTHESIS,
                    name, "1", "test", features, true);
        }

        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(new EngineId(id), List.of());
        }

        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return readiness;
        }

        @Override public VoiceSynthesisResult synthesize(VoiceSynthesisRequest request,
                                                         ExecutionContext context) throws IOException {
            throw new IOException("not used");
        }
    }
}
