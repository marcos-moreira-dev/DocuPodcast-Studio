package com.marcosmoreiradev.docupodcaststudio.bootstrap;

/** Window sizing policy for the desktop application. */
public record ApplicationWindowConfig(
        double defaultWidth,
        double defaultHeight,
        double minimumWidth,
        double minimumHeight
) {
    public static ApplicationWindowConfig defaultConfig() {
        return new ApplicationWindowConfig(1360, 820, 1024, 640);
    }
}
