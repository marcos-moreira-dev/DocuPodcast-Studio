package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
/** Assets created when an imported document is materialized into a project folder. */
public record MaterializedImportedDocument(
        ProjectAssetReference sourceDocumentAsset,
        ProjectAssetReference importedDocumentAsset,
        ProjectDocumentSource projectSource
) {
    public MaterializedImportedDocument(ProjectAssetReference sourceDocumentAsset, ProjectAssetReference importedDocumentAsset) {
        this(sourceDocumentAsset, importedDocumentAsset, null);
    }
}
