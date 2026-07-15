package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Copies/saves the imported document representation into the project folder and registers assets. */
public final class MaterializeImportedDocumentUseCase {
    private final ImportedDocumentWorkspaceRepository repository;

    public MaterializeImportedDocumentUseCase(ImportedDocumentWorkspaceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public DocuPodcastProject materialize(DocuPodcastProject project, ReadableDocument document, Path projectFile) throws IOException {
        return materializeWithResult(project, document, projectFile).project();
    }

    public MaterializedImportedDocumentResult materializeWithResult(DocuPodcastProject project, ReadableDocument document, Path projectFile) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(projectFile, "projectFile");
        MaterializedImportedDocument materialized = repository.materialize(document, project.readingProfile(), projectFile);
        ReadableDocument projectSourceDocument = materialized.projectSourceDocument() == null ? document : materialized.projectSourceDocument();
        DocuPodcastProject updated = project
                .withAsset(materialized.sourceDocumentAsset())
                .withAsset(materialized.importedDocumentAsset());
        return new MaterializedImportedDocumentResult(updated, projectSourceDocument, materialized);
    }
}
