package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Lists persisted jobs from jobs/JOB-xxx/job.json without starting a generation engine. */
public final class ListPersistedAudioJobsUseCase {
    private final AudioJobRepository repository;

    public ListPersistedAudioJobsUseCase(AudioJobRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<AudioJobSnapshot> list(Path projectDirectory) throws IOException {
        return repository.list(projectDirectory);
    }
}
