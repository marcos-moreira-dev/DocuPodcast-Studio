package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;

import java.util.Map;
import java.util.Objects;

/** Associates a user-selected image asset with a narration segment. */
public final class BindImageToSegmentUseCase {
    public StoryboardDocument bind(StoryboardDocument storyboard,
                                   NarrationScriptDocument script,
                                   ProjectAssetCatalog assets,
                                   String segmentId,
                                   String imageAssetId,
                                   String caption,
                                   StoryboardDisplayMode displayMode) {
        Objects.requireNonNull(storyboard, "storyboard");
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(assets, "assets");
        String normalizedSegment = required(segmentId, "segmentId");
        String normalizedImage = required(imageAssetId, "imageAssetId");
        script.segmentById(normalizedSegment).orElseThrow(() ->
                new IllegalArgumentException("No existe el segmento para storyboard: " + normalizedSegment));
        assets.byId(normalizedImage)
                .filter(asset -> asset.kind() == ProjectAssetKind.IMAGE)
                .orElseThrow(() -> new IllegalArgumentException("No existe imagen registrada: " + normalizedImage));
        String bindingId = "STB-" + normalizedSegment.replaceFirst("^SEG-", "");
        StoryboardBinding binding = new StoryboardBinding(
                bindingId,
                normalizedSegment,
                normalizedImage,
                displayMode == null ? StoryboardDisplayMode.FIT_CONTAIN : displayMode,
                caption,
                Map.of("source", "user-associated-image")
        );
        return storyboard.withBinding(binding);
    }

    private static String required(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}
