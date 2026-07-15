package com.marcosmoreiradev.docupodcaststudio.domain.storyboard;

/** Lightweight scene projection used by the Storyboard workspace. */
public record StoryboardScene(
        String id,
        String segmentId,
        String title,
        String narrationPreview,
        String imageAssetId,
        String caption,
        StoryboardDisplayMode displayMode,
        boolean hasImage
) {
    public StoryboardScene {
        id = requireToken(id, "id");
        segmentId = requireToken(segmentId, "segmentId");
        title = title == null || title.isBlank() ? segmentId : title.strip();
        narrationPreview = narrationPreview == null ? "" : narrationPreview.strip();
        imageAssetId = imageAssetId == null ? "" : imageAssetId.strip();
        caption = caption == null ? "" : caption.strip();
        displayMode = displayMode == null ? StoryboardDisplayMode.FIT_CONTAIN : displayMode;
    }

    public String statusLabel() {
        return hasImage ? "Imagen asociada: " + imageAssetId : "Sin imagen asociada";
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
