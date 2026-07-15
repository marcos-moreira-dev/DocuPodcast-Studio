package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoResolutionPreset;

/** Visual defaults for Estudio documental text+audio video frames. */
public record DocumentTextVideoOptions(
        SimpleVideoResolutionPreset resolution,
        DocumentTextVideoBackgroundMode backgroundMode,
        String backgroundColor,
        String backgroundImagePath,
        String textColor,
        String accentColor,
        String fontFamily,
        int fontSize
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
        fontSize = Math.max(18, fontSize);
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
                54);
    }

    public DocumentTextVideoOptions withResolution(SimpleVideoResolutionPreset nextResolution) {
        return new DocumentTextVideoOptions(nextResolution, backgroundMode, backgroundColor, backgroundImagePath,
                textColor, accentColor, fontFamily, fontSize);
    }

    private static String colorOrDefault(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.matches("#[0-9A-Fa-f]{6}") ? normalized : fallback;
    }
}
