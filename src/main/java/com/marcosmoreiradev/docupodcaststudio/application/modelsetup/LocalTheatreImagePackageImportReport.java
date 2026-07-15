package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Result of importing a local theatre image model package. */
public record LocalTheatreImagePackageImportReport(
        boolean success,
        Path sourceFolder,
        Path targetFolder,
        ModelInspectionResult inspection,
        List<String> copiedFiles,
        String userMessage
) {
    public LocalTheatreImagePackageImportReport {
        copiedFiles = List.copyOf(copiedFiles == null ? List.of() : copiedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }
}
