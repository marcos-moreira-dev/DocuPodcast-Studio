package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.io.IOException;
import java.util.Objects;

/** Assigns a generated image to its own storyboard slot without destroying other variants. */
public final class UpsertTheatreGeneratedFrameVariantUseCase {
    private final UpsertTheatreStoryboardFrameVariantUseCase variants;

    public UpsertTheatreGeneratedFrameVariantUseCase(UpsertTheatreStoryboardFrameVariantUseCase variants) {
        this.variants = Objects.requireNonNull(variants, "variants");
    }

    public UpsertTheatreStoryboardFrameVariantUseCase.Result execute(
            DocuPodcastProject project,
            StoryboardDocument storyboard,
            NarrationScriptDocument script,
            String segmentId,
            ProjectAssetReference generatedAsset,
            boolean activate) throws IOException {
        return variants.upsertGeneratedFrame(project, storyboard, script, segmentId, generatedAsset, activate);
    }
}
