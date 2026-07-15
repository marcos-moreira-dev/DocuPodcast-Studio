package com.marcosmoreiradev.docupodcaststudio.application.video;

/** User-facing visual options for the composed theatre-work MP4 frames. */
public record TheatreWorkVideoOptions(
        boolean useFragmentImages,
        boolean showText,
        String fontFamily,
        int fontSize,
        String textColor,
        TextPosition textPosition,
        TextEffect textEffect,
        FrameLayout frameLayout,
        boolean showSpatialMap,
        boolean showCharacters,
        boolean showDisplacements,
        String backgroundColor
) {
    public TheatreWorkVideoOptions {
        fontFamily = normalize(fontFamily, "Inter");
        fontSize = Math.max(12, fontSize);
        textColor = normalize(textColor, "#111827");
        textPosition = textPosition == null ? TextPosition.CENTER : textPosition;
        textEffect = textEffect == null ? TextEffect.NONE : textEffect;
        frameLayout = frameLayout == null ? FrameLayout.IMAGE_WITH_TEXT : frameLayout;
        backgroundColor = normalize(backgroundColor, "#FFFFFF");
    }

    public static TheatreWorkVideoOptions defaults() {
        return new TheatreWorkVideoOptions(true, true, "Inter", 42, "#111827", TextPosition.CENTER,
                TextEffect.SHADOW, FrameLayout.IMAGE_WITH_TEXT, false, true, true, "#FFFFFF");
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    public enum TextPosition {
        TOP,
        CENTER,
        BOTTOM,
        LEFT,
        RIGHT
    }

    public enum TextEffect {
        NONE,
        SHADOW,
        SOLID_BORDER
    }

    public enum FrameLayout {
        TEXT_ONLY,
        IMAGE_OR_TEXT,
        IMAGE_WITH_TEXT,
        IMAGE_WITH_SPATIAL_MAP
    }
}
