package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

final class TestVoiceEngine implements VoiceSynthesisEngine {
    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(new EngineId("test-voice"),
                CapabilityId.VOICE_SYNTHESIS, "Test", "1", "test",
                Set.of(), true);
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
        return EngineReadiness.ready(descriptor().id(), "ready");
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(descriptor().id(), java.util.List.of());
    }

    @Override public VoiceSynthesisResult synthesize(
            VoiceSynthesisRequest request, ExecutionContext context)
            throws java.io.IOException {
        Files.write(request.outputFile(), new byte[44]);
        return new VoiceSynthesisResult(request.outputFile(), 0, Map.of());
    }
}
