package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

/** Result of importing an image for live storyboard usage. */
public record ImageAssetImportResult(DocuPodcastProject project, ProjectAssetReference imageAsset) {
}
