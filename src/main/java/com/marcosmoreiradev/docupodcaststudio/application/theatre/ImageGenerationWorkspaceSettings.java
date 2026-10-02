package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.nio.file.Path;
import java.time.Duration;

/** Operational limits and output location for a provider-neutral image workspace. */
public record ImageGenerationWorkspaceSettings(Duration timeout, Path outputDirectory) {
    public ImageGenerationWorkspaceSettings {
        timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? Duration.ofSeconds(20)
                : timeout;
    }

    public static ImageGenerationWorkspaceSettings defaults(Path outputDirectory) {
        return new ImageGenerationWorkspaceSettings(Duration.ofSeconds(20), outputDirectory);
    }
}
