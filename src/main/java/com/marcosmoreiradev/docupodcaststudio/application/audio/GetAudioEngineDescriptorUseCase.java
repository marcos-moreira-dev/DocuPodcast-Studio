package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.Objects;

/** Returns user-facing diagnostics for the currently configured audio generation engine. */
public final class GetAudioEngineDescriptorUseCase {
    private final AudioGenerationGateway gateway;

    public GetAudioEngineDescriptorUseCase(AudioGenerationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public AudioEngineDescriptor get() {
        return gateway.engineDescriptor();
    }
}
