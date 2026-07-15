package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Saves the internal prepared-reading snapshot and registers it as a project asset. */
public final class MaterializeNarrationScriptUseCase {
    private final NarrationScriptWorkspaceRepository repository;

    public MaterializeNarrationScriptUseCase(NarrationScriptWorkspaceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public DocuPodcastProject materialize(DocuPodcastProject project, NarrationScriptDocument script, Path projectFile) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(projectFile, "projectFile");
        MaterializedNarrationScript materialized = repository.materialize(script, projectFile);
        return project.withAsset(materialized.narrationScriptAsset());
    }
}
