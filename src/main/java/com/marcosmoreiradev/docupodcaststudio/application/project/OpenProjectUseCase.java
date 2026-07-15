package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Opens a DocuPodcast project through the repository port. */
public final class OpenProjectUseCase {
    private final ProjectRepository repository;

    public OpenProjectUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public DocuPodcastProject open(Path sourceFile) throws IOException {
        return repository.open(Objects.requireNonNull(sourceFile, "sourceFile"));
    }
}
