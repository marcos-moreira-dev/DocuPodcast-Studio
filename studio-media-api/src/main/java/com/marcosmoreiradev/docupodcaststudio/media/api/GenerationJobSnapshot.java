package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GenerationJobSnapshot(
        GenerationJobRequest request,
        GenerationJobStatus status,
        String stage,
        double progress,
        String message,
        List<GenerationArtifact> artifacts,
        String diagnostic,
        int attempt,
        Instant updatedAt) {
    public GenerationJobSnapshot {
        request = Objects.requireNonNull(request, "request");
        status = Objects.requireNonNullElse(status, GenerationJobStatus.QUEUED);
        stage = stage == null ? "" : stage.strip();
        progress = Math.max(0.0, Math.min(1.0, progress));
        message = message == null ? "" : message.strip();
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        diagnostic = diagnostic == null ? "" : diagnostic.strip();
        attempt = Math.max(0, attempt);
        updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    public static GenerationJobSnapshot queued(GenerationJobRequest request) {
        return new GenerationJobSnapshot(request, GenerationJobStatus.QUEUED, "queued", 0, "",
                List.of(), "", 0, Instant.now());
    }
}
