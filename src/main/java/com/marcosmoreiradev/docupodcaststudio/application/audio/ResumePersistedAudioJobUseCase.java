package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.util.Objects;
import java.util.function.Consumer;

/** Resumes a persisted audio job by delegating to the active audio generation gateway. */
public final class ResumePersistedAudioJobUseCase {
    private final AudioGenerationGateway gateway;

    public ResumePersistedAudioJobUseCase(AudioGenerationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(snapshot, "snapshot");
        return gateway.resume(request, snapshot, statusConsumer);
    }
}
