package com.marcosmoreiradev.docupodcaststudio.application.settings;

import java.util.Locale;

/** Persistent defaults for theatrical frame batch generation. */
public record FrameGenerationSettings(
        String mode,
        String scope,
        String outputDirectory,
        String overwritePolicy
) {
    public FrameGenerationSettings {
        mode = clean(mode, "SINGLE").toUpperCase(Locale.ROOT);
        scope = clean(scope, "ALL").toUpperCase(Locale.ROOT);
        outputDirectory = clean(outputDirectory, "");
        overwritePolicy = clean(overwritePolicy, "UNIQUE").toUpperCase(Locale.ROOT);
    }

    public static FrameGenerationSettings defaults() {
        return new FrameGenerationSettings("SINGLE", "ALL", "", "UNIQUE");
    }

    private static String clean(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
