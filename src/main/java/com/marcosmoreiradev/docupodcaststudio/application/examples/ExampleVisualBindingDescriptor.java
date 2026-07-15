package com.marcosmoreiradev.docupodcaststudio.application.examples;

/** Predefined visual assignment for a bundled demo example. */
public record ExampleVisualBindingDescriptor(
        String assetFileName,
        int fragmentIndex,
        String anchorText,
        String suggestedCaption
) {
    public ExampleVisualBindingDescriptor(String assetFileName, int fragmentIndex, String suggestedCaption) {
        this(assetFileName, fragmentIndex, "", suggestedCaption);
    }

    public ExampleVisualBindingDescriptor {
        assetFileName = require(assetFileName, "assetFileName");
        if (fragmentIndex < 1) {
            throw new IllegalArgumentException("fragmentIndex must be >= 1");
        }
        anchorText = anchorText == null ? "" : anchorText.strip();
        suggestedCaption = suggestedCaption == null ? "" : suggestedCaption.strip();
    }

    public boolean hasAnchorText() {
        return !anchorText.isBlank();
    }

    private static String require(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}
