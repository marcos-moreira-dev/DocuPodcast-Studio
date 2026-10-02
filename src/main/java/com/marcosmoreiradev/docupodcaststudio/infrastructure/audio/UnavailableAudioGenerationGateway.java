package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationGateway;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.errors.EngineUnavailableException;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.util.List;
import java.util.function.Consumer;

/** Honest production fallback: missing configuration never generates a fake success artifact. */
public final class UnavailableAudioGenerationGateway implements AudioGenerationGateway {
    private final AudioEngineDescriptor descriptor;

    public UnavailableAudioGenerationGateway(String engineId, String displayName, String message) {
        this.descriptor = AudioEngineDescriptor.unavailable(engineId, displayName, message);
    }

    @Override public AudioEngineDescriptor engineDescriptor() { return descriptor; }

    @Override public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        throw unavailable();
    }

    @Override public String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot,
                                   Consumer<AudioJobStatusDto> statusConsumer) {
        throw unavailable();
    }

    @Override public boolean cancel(String jobId) { return false; }
    @Override public List<AudioJobStatusDto> listStatuses() { return List.of(); }

    private EngineUnavailableException unavailable() {
        return new EngineUnavailableException(descriptor.displayName(), descriptor.message(), descriptor.engineId());
    }
}
