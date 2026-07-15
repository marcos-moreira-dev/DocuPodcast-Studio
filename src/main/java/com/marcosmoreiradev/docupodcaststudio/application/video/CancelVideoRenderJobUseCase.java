package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Requests safe cancellation of a persistent FFmpeg render job. */
public final class CancelVideoRenderJobUseCase {
    private final VideoRenderJobRepository repository;

    public CancelVideoRenderJobUseCase(VideoRenderJobRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public VideoRenderJobSnapshot cancel(Path projectDirectory, String jobId, String reason) throws IOException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        VideoRenderJobSnapshot current = repository.load(projectDirectory, jobId)
                .orElseThrow(() -> new IOException("No existe el job de video " + jobId));
        VideoRenderJobSnapshot cancelled = current.cancelled(reason == null || reason.isBlank()
                ? "Cancelado por el usuario desde la interfaz de render." : reason);
        repository.save(projectDirectory, cancelled, null);
        return cancelled;
    }
}
