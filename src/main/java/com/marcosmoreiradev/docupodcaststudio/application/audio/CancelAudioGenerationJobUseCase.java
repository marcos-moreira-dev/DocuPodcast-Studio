package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.time.Duration;
import java.util.Objects;

/** Requests cooperative cancellation of an audio generation job. */
public final class CancelAudioGenerationJobUseCase {
    private final AudioGenerationGateway gateway;

    public CancelAudioGenerationJobUseCase(AudioGenerationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public boolean cancel(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return false;
        }
        return gateway.cancel(jobId.strip());
    }

    public boolean cancelAndAwait(String jobId, Duration timeout) throws InterruptedException {
        if (jobId == null || jobId.isBlank()) return true;
        String target = jobId.strip();
        gateway.cancel(target);
        return gateway.awaitTermination(target, timeout == null ? Duration.ofSeconds(30) : timeout);
    }
}
