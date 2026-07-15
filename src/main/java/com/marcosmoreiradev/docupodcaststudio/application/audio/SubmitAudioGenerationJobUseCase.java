package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.application.process.AiModelResourceGuard;
import com.marcosmoreiradev.docupodcaststudio.application.process.AiModelResourceGuard.AiModelResourceKind;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Starts a background audio generation job. */
public final class SubmitAudioGenerationJobUseCase {
    private final AudioGenerationGateway gateway;
    private final AiModelResourceGuard resourceGuard;

    public SubmitAudioGenerationJobUseCase(AudioGenerationGateway gateway) {
        this(gateway, AiModelResourceGuard.global());
    }

    public SubmitAudioGenerationJobUseCase(AudioGenerationGateway gateway, AiModelResourceGuard resourceGuard) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.resourceGuard = Objects.requireNonNull(resourceGuard, "resourceGuard");
    }

    public String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer) {
        AudioGenerationRequest safeRequest = Objects.requireNonNull(request, "request");
        AiModelResourceGuard.Lease lease = resourceGuard.acquire(AiModelResourceKind.AUDIO, safeRequest.jobName());
        AtomicBoolean released = new AtomicBoolean(false);
        Consumer<AudioJobStatusDto> guardedConsumer = status -> {
            if (statusConsumer != null) {
                statusConsumer.accept(status);
            }
            if (status != null && !status.running() && released.compareAndSet(false, true)) {
                lease.close();
            }
        };
        try {
            return gateway.submit(safeRequest, guardedConsumer);
        } catch (RuntimeException ex) {
            if (released.compareAndSet(false, true)) {
                lease.close();
            }
            throw ex;
        }
    }
}
