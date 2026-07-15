package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.application.document.ImportedDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.script.NarrationScriptWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.StoryboardWorkspaceRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Loads materialized document/script/storyboard snapshots from a project folder during openProject. */
public final class LoadProjectWorkspaceArtifactsUseCase {
    private final ImportedDocumentWorkspaceRepository importedDocumentRepository;
    private final NarrationScriptWorkspaceRepository narrationScriptRepository;
    private final StoryboardWorkspaceRepository storyboardRepository;

    public LoadProjectWorkspaceArtifactsUseCase(
            ImportedDocumentWorkspaceRepository importedDocumentRepository,
            NarrationScriptWorkspaceRepository narrationScriptRepository,
            StoryboardWorkspaceRepository storyboardRepository
    ) {
        this.importedDocumentRepository = Objects.requireNonNull(importedDocumentRepository, "importedDocumentRepository");
        this.narrationScriptRepository = Objects.requireNonNull(narrationScriptRepository, "narrationScriptRepository");
        this.storyboardRepository = Objects.requireNonNull(storyboardRepository, "storyboardRepository");
    }

    public ProjectWorkspaceHydration load(Path projectFile) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        return new ProjectWorkspaceHydration(
                importedDocumentRepository.load(projectFile),
                narrationScriptRepository.load(projectFile),
                storyboardRepository.load(projectFile)
        );
    }
}
