package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;

/** Result of preparing local theatre image runtime folders. */
public record LocalTheatreImagePreparationReport(
        boolean success,
        Path runtimeDirectory,
        Path modelDirectory,
        String userMessage
) {
    public LocalTheatreImagePreparationReport {
        userMessage = userMessage == null ? "" : userMessage.strip();
    }
}
