package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.util.Objects;
import java.util.function.Consumer;

/** Starts a background audio generation job. */
public final class SubmitAudioGenerationJobUseCase {
    private final AudioGenerationGateway gateway;

    public SubmitAudioGenerationJobUseCase(AudioGenerationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        AudioGenerationRequest safeRequest = Objects.requireNonNull(request, "request");
        return gateway.submit(safeRequest, statusConsumer);
    }
}
