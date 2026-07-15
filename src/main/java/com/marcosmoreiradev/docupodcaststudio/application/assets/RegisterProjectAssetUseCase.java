package com.marcosmoreiradev.docupodcaststudio.application.assets;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.util.Objects;

/** Adds or replaces a project asset reference. */
public final class RegisterProjectAssetUseCase {
    public DocuPodcastProject register(DocuPodcastProject project, ProjectAssetReference reference) {
        return Objects.requireNonNull(project, "project").withAsset(Objects.requireNonNull(reference, "reference"));
    }
}
