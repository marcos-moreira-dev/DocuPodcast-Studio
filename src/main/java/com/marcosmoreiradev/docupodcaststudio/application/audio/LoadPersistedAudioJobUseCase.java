package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Loads the full persisted detail of one audio job. */
public final class LoadPersistedAudioJobUseCase {
    private final AudioJobRepository repository;

    public LoadPersistedAudioJobUseCase(AudioJobRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<AudioJobSnapshot> load(Path projectDirectory, String jobId) throws IOException {
        return repository.load(projectDirectory, jobId);
    }
}
