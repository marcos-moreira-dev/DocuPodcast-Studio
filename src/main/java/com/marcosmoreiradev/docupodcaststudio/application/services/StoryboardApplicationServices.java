package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.storyboard.BindImageToSegmentUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.BuildStoryboardFromScriptUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.BuildStoryboardFromImageLayersUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ImportImageAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.MaterializeStoryboardUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreStoryboardFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.UpsertTheatreGeneratedFrameVariantUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.ValidateStoryboardUseCase;

/** Application services for live storyboard manifests and image bindings. */
public record StoryboardApplicationServices(
        BuildStoryboardFromScriptUseCase buildStoryboardFromScript,
        BuildStoryboardFromImageLayersUseCase buildStoryboardFromImageLayers,
        BindImageToSegmentUseCase bindImageToSegment,
        ImportImageAssetUseCase importImageAsset,
        UpsertTheatreStoryboardFrameVariantUseCase upsertTheatreStoryboardFrameVariant,
        UpsertTheatreGeneratedFrameVariantUseCase upsertTheatreGeneratedFrameVariant,
        ValidateStoryboardUseCase validateStoryboard,
        MaterializeStoryboardUseCase materializeStoryboard
) {
}
