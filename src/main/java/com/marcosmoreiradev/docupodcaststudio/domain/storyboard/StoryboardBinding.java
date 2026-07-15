package com.marcosmoreiradev.docupodcaststudio.domain.storyboard;

import java.util.Map;
import java.util.Objects;

/**
 * Association between a narration segment and a user-provided visual asset.
 *
 * <p>The application does not infer or generate the image. The user decides which image belongs to
 * each segment, and this binding preserves that decision for playback/export.</p>
 */
public record StoryboardBinding(
        String id,
        String segmentId,
        String imageAssetId,
        StoryboardDisplayMode displayMode,
        String caption,
        Map<String, String> metadata
) {
    public StoryboardBinding {
        id = requireToken(id, "id");
        segmentId = requireToken(segmentId, "segmentId");
        imageAssetId = requireToken(imageAssetId, "imageAssetId");
        displayMode = Objects.requireNonNullElse(displayMode, StoryboardDisplayMode.FIT_CONTAIN);
        caption = caption == null ? "" : caption.strip();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static StoryboardBinding of(String id, String segmentId, String imageAssetId, String caption) {
        return new StoryboardBinding(id, segmentId, imageAssetId, StoryboardDisplayMode.FIT_CONTAIN, caption, Map.of());
    }

    public StoryboardBinding withImage(String nextImageAssetId, String nextCaption, StoryboardDisplayMode nextMode) {
        return new StoryboardBinding(id, segmentId, nextImageAssetId, nextMode, nextCaption, metadata);
    }

    public StoryboardBinding withImageAndMetadata(String nextImageAssetId,
                                                  String nextCaption,
                                                  StoryboardDisplayMode nextMode,
                                                  Map<String, String> nextMetadata) {
        return new StoryboardBinding(id, segmentId, nextImageAssetId, nextMode, nextCaption, nextMetadata);
    }

    public StoryboardBinding withMetadata(Map<String, String> nextMetadata) {
        return new StoryboardBinding(id, segmentId, imageAssetId, displayMode, caption, nextMetadata);
    }

    private static String requireToken(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }
}
