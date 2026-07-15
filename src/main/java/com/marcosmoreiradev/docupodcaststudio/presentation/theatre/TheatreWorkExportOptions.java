package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.presentation.video.VideoExportOptions;

/** User-selected visual options for theatrical video export. */
public record TheatreWorkExportOptions(
        VideoExportOptions videoOptions,
        boolean useFragmentImages,
        boolean showText,
        String fontFamily,
        int fontSize,
        String textColor,
        TheatreTextPosition textPosition,
        TheatreTextEffect textEffect,
        TheatreFrameLayout frameLayout,
        boolean showSpatialMap,
        boolean showCharacters,
        boolean showDisplacements,
        String backgroundColor
) {
    public TheatreWorkExportOptions {
        fontFamily = blankToDefault(fontFamily, "Inter");
        fontSize = Math.max(12, fontSize);
        textColor = blankToDefault(textColor, "#111827");
        textPosition = textPosition == null ? TheatreTextPosition.CENTER : textPosition;
        textEffect = textEffect == null ? TheatreTextEffect.NONE : textEffect;
        frameLayout = frameLayout == null ? TheatreFrameLayout.IMAGE_WITH_TEXT : frameLayout;
        backgroundColor = blankToDefault(backgroundColor, "#FFFFFF");
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
