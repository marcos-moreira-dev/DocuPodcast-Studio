package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

/** Creates a new in-memory DocuPodcast project. */
public final class CreateProjectUseCase {
    public DocuPodcastProject create(String title) {
        return DocuPodcastProject.createNew(title);
    }

    public DocuPodcastProject create(String title, ProjectMode mode) {
        return DocuPodcastProject.createNew(title, mode);
    }
}
