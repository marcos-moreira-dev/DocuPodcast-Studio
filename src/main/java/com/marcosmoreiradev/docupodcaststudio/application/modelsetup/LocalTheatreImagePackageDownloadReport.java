package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Result of the explicit heavy image package download. */
public record LocalTheatreImagePackageDownloadReport(
        boolean success,
        Path targetFolder,
        ModelInspectionResult inspection,
        List<String> downloadedFiles,
        String userMessage
) {
    public LocalTheatreImagePackageDownloadReport {
        downloadedFiles = List.copyOf(downloadedFiles == null ? List.of() : downloadedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }
}
