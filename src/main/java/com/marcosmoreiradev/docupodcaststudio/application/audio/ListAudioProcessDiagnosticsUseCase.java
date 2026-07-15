package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Reads persisted process diagnostics for a single audio job. */
public final class ListAudioProcessDiagnosticsUseCase {
    private final AudioProcessDiagnosticsRepository repository;

    public ListAudioProcessDiagnosticsUseCase(AudioProcessDiagnosticsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<AudioProcessDiagnosticEvent> list(Path projectDirectory, String jobId) throws IOException {
        return repository.list(projectDirectory, jobId);
    }
}
