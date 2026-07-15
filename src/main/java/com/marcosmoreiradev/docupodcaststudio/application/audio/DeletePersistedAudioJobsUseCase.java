package com.marcosmoreiradev.docupodcaststudio.application.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Deletes persisted audio jobs when a fresh render must not reuse previous voice chunks. */
public final class DeletePersistedAudioJobsUseCase {
    private final AudioJobRepository repository;

    public DeletePersistedAudioJobsUseCase(AudioJobRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void delete(Path projectDirectory) throws IOException {
        repository.deleteAll(projectDirectory);
    }
}
