package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Writes storyboard/storyboard.json and registers the manifest asset. */
public final class MaterializeStoryboardUseCase {
    private final StoryboardWorkspaceRepository repository;

    public MaterializeStoryboardUseCase(StoryboardWorkspaceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public DocuPodcastProject materialize(DocuPodcastProject project, StoryboardDocument storyboard, Path projectFile) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(storyboard, "storyboard");
        MaterializedStoryboard materialized = repository.materialize(projectFile, storyboard);
        return project.withAsset(materialized.asset());
    }
}
