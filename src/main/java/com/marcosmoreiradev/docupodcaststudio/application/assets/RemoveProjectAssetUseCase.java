package com.marcosmoreiradev.docupodcaststudio.application.assets;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.util.Objects;

/** Removes a project asset reference by ID. */
public final class RemoveProjectAssetUseCase {
    public DocuPodcastProject remove(DocuPodcastProject project, String assetId) {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId is required");
        }
        return Objects.requireNonNull(project, "project").withoutAsset(assetId);
    }
}
