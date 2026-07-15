package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.nio.file.Path;

/** Persistence port for DocuPodcast projects. */
public interface ProjectRepository {
    void save(DocuPodcastProject project, Path targetFile) throws IOException;

    DocuPodcastProject open(Path sourceFile) throws IOException;
}
