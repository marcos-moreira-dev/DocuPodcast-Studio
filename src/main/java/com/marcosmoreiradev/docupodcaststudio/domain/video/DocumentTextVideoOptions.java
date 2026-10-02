package com.marcosmoreiradev.docupodcaststudio.domain.video;

import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;

/** Visual defaults for Estudio documental text+audio video frames. */
public record DocumentTextVideoOptions(
        SimpleVideoResolutionPreset resolution,
        DocumentTextVideoBackgroundMode backgroundMode,
        String backgroundColor,
        String backgroundImagePath,
        String textColor,
        String accentColor,
        String fontFamily,
        String titleFontFamily,
        int fontSize,
        boolean underlineNarratedText,
        String narratedUnderlineColor,
        int narratedUnderlineThicknessPx,
        double backgroundImageOpacity,
        DocumentBackgroundImageFit backgroundImageFit,
        DocumentTextEffect textEffect,
        String textEffectColor,
        int textEffectThicknessPx
) {
    public DocumentTextVideoOptions {
        resolution = resolution == null ? SimpleVideoResolutionPreset.defaultPreset() : resolution;
        backgroundMode = backgroundMode == null ? DocumentTextVideoBackgroundMode.SOLID_COLOR : backgroundMode;
        backgroundColor = colorOrDefault(backgroundColor, "#FFFFFF");
        backgroundImagePath = backgroundImagePath == null ? "" : backgroundImagePath.strip();
        if (backgroundMode == DocumentTextVideoBackgroundMode.IMAGE && backgroundImagePath.isBlank()) {
            backgroundMode = DocumentTextVideoBackgroundMode.SOLID_COLOR;
        }
        textColor = colorOrDefault(textColor, "#20232A");
        accentColor = colorOrDefault(accentColor, "#4F46E5");
        fontFamily = fontFamily == null || fontFamily.isBlank() ? "SansSerif" : fontFamily.strip();
        titleFontFamily = titleFontFamily == null || titleFontFamily.isBlank()
                ? fontFamily : titleFontFamily.strip();
        fontSize = Math.max(18, fontSize);
        narratedUnderlineColor = colorOrDefault(narratedUnderlineColor, "#4F46E5");
        narratedUnderlineThicknessPx = Math.max(0, Math.min(15, narratedUnderlineThicknessPx));
        backgroundImageOpacity = Math.max(0.05, Math.min(1.0, backgroundImageOpacity));
        backgroundImageFit = backgroundImageFit == null ? DocumentBackgroundImageFit.COVER : backgroundImageFit;
        textEffect = textEffect == null ? DocumentTextEffect.NONE : textEffect;
        textEffectColor = colorOrDefault(textEffectColor, "#000000");
        textEffectThicknessPx = Math.max(1, Math.min(12, textEffectThicknessPx));
    }

    /** Compatibility constructor for projects created before background fitting was configurable. */
    public DocumentTextVideoOptions(
            SimpleVideoResolutionPreset resolution, DocumentTextVideoBackgroundMode backgroundMode,
            String backgroundColor, String backgroundImagePath, String textColor, String accentColor,
            String fontFamily, String titleFontFamily, int fontSize, boolean underlineNarratedText,
            String narratedUnderlineColor, int narratedUnderlineThicknessPx, double backgroundImageOpacity,
            DocumentTextEffect textEffect, String textEffectColor, int textEffectThicknessPx
    ) {
        this(resolution, backgroundMode, backgroundColor, backgroundImagePath, textColor, accentColor,
                fontFamily, titleFontFamily, fontSize, underlineNarratedText, narratedUnderlineColor,
                narratedUnderlineThicknessPx, backgroundImageOpacity, DocumentBackgroundImageFit.COVER,
                textEffect, textEffectColor, textEffectThicknessPx);
    }

    /** Compatibility constructor for projects created before title typography was independent. */
    public DocumentTextVideoOptions(
            SimpleVideoResolutionPreset resolution,
            DocumentTextVideoBackgroundMode backgroundMode,
            String backgroundColor,
            String backgroundImagePath,
            String textColor,
            String accentColor,
            String fontFamily,
            int fontSize,
            boolean underlineNarratedText,
            String narratedUnderlineColor,
            int narratedUnderlineThicknessPx,
            double backgroundImageOpacity,
            DocumentTextEffect textEffect,
            String textEffectColor,
            int textEffectThicknessPx
    ) {
        this(resolution, backgroundMode, backgroundColor, backgroundImagePath,
                textColor, accentColor, fontFamily, fontFamily, fontSize,
                underlineNarratedText, narratedUnderlineColor, narratedUnderlineThicknessPx,
                backgroundImageOpacity, DocumentBackgroundImageFit.COVER,
                textEffect, textEffectColor, textEffectThicknessPx);
    }

    /** Compatibility constructor for persisted projects created before image opacity and text effects. */
    public DocumentTextVideoOptions(
            SimpleVideoResolutionPreset resolution,
            DocumentTextVideoBackgroundMode backgroundMode,
            String backgroundColor,
            String backgroundImagePath,
            String textColor,
            String accentColor,
            String fontFamily,
            int fontSize,
            boolean underlineNarratedText,
            String narratedUnderlineColor,
            int narratedUnderlineThicknessPx
    ) {
        this(resolution, backgroundMode, backgroundColor, backgroundImagePath,
                textColor, accentColor, fontFamily, fontFamily, fontSize,
                underlineNarratedText, narratedUnderlineColor, narratedUnderlineThicknessPx,
                0.35, DocumentBackgroundImageFit.COVER, DocumentTextEffect.NONE, "#000000", 3);
    }

    /** Compatibility constructor for callers created before narrated-text emphasis was configurable. */
    public DocumentTextVideoOptions(
            SimpleVideoResolutionPreset resolution,
            DocumentTextVideoBackgroundMode backgroundMode,
            String backgroundColor,
            String backgroundImagePath,
            String textColor,
            String accentColor,
            String fontFamily,
            int fontSize
    ) {
        this(resolution, backgroundMode, backgroundColor, backgroundImagePath,
                textColor, accentColor, fontFamily, fontFamily, fontSize,
                true, "#4F46E5", 4,
                0.35, DocumentBackgroundImageFit.COVER, DocumentTextEffect.NONE, "#000000", 3);
    }

    public static DocumentTextVideoOptions defaults() {
        return new DocumentTextVideoOptions(
                SimpleVideoResolutionPreset.defaultPreset(),
                DocumentTextVideoBackgroundMode.SOLID_COLOR,
                "#FFFFFF",
                "",
                "#20232A",
                "#4F46E5",
                "SansSerif",
                "SansSerif",
                54,
                true,
                "#4F46E5",
                4,
                0.35,
                DocumentBackgroundImageFit.COVER,
                DocumentTextEffect.NONE,
                "#000000",
                3);
    }

    public DocumentTextVideoOptions withBackgroundImage(String path, Double visibility) {
        return withBackgroundImage(path, visibility, backgroundImageFit);
    }

    public DocumentTextVideoOptions withBackgroundImage(String path, Double visibility, DocumentBackgroundImageFit fit) {
        return new DocumentTextVideoOptions(resolution, DocumentTextVideoBackgroundMode.IMAGE, backgroundColor, path,
                textColor, accentColor, fontFamily, titleFontFamily, fontSize, underlineNarratedText,
                narratedUnderlineColor, narratedUnderlineThicknessPx, visibility == null ? backgroundImageOpacity : visibility,
                fit, textEffect, textEffectColor, textEffectThicknessPx);
    }

    public DocumentTextVideoOptions withResolution(SimpleVideoResolutionPreset nextResolution) {
        return new DocumentTextVideoOptions(nextResolution, backgroundMode, backgroundColor, backgroundImagePath,
                textColor, accentColor, fontFamily, titleFontFamily, fontSize,
                underlineNarratedText, narratedUnderlineColor, narratedUnderlineThicknessPx,
                backgroundImageOpacity, backgroundImageFit, textEffect, textEffectColor, textEffectThicknessPx);
    }

    private static String colorOrDefault(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.matches("#[0-9A-Fa-f]{6}") ? normalized : fallback;
    }
}
