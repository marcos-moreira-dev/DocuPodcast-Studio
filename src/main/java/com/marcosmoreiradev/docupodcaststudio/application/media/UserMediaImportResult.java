package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

/** Result of importing audio directly or extracting audio from video. */
public record UserMediaImportResult(
        DocuPodcastProject project,
        UserMediaImportStatus status,
        ProjectAssetReference audioAsset,
        ProjectAssetReference originalVideoAsset,
        String message
) {
    public UserMediaImportResult {
        message = message == null ? "" : message.strip();
    }

    public boolean extractedFromVideo() {
        return originalVideoAsset != null;
    }
}
