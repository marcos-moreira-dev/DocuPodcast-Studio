package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Saves a DocuPodcast project through the repository port. */
public final class SaveProjectUseCase {
    private final ProjectRepository repository;

    public SaveProjectUseCase(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void save(DocuPodcastProject project, Path targetFile) throws IOException {
        repository.save(Objects.requireNonNull(project, "project"), Objects.requireNonNull(targetFile, "targetFile"));
    }
}
