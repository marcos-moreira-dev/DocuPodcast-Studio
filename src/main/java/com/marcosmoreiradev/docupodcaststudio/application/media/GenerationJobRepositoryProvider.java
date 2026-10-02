package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobRepository;

import java.nio.file.Path;

/** Opens the project-scoped neutral job store without exposing filesystem infrastructure. */
@FunctionalInterface
public interface GenerationJobRepositoryProvider {
    GenerationJobRepository forProject(Path projectRoot);
}
