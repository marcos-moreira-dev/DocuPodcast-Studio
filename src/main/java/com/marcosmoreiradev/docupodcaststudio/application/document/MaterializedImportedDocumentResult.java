package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

/** Project state plus the canonical project-local source document after materialization. */
public record MaterializedImportedDocumentResult(
        DocuPodcastProject project,
        ReadableDocument projectSourceDocument,
        MaterializedImportedDocument materialized
) {
}
