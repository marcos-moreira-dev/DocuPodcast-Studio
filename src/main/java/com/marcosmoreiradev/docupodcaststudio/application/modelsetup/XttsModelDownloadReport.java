package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Result of downloading the official advanced AI voice model files into the local runtime layout. */
public record XttsModelDownloadReport(
        boolean success,
        Path targetFolder,
        List<String> downloadedFiles,
        List<String> skippedFiles,
        List<String> failedFiles,
        ModelInspectionResult inspection,
        String userMessage,
        Path diagnosticReport
) {
    public XttsModelDownloadReport {
        downloadedFiles = List.copyOf(downloadedFiles == null ? List.of() : downloadedFiles);
        skippedFiles = List.copyOf(skippedFiles == null ? List.of() : skippedFiles);
        failedFiles = List.copyOf(failedFiles == null ? List.of() : failedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public XttsModelDownloadReport(boolean success, Path targetFolder, List<String> downloadedFiles,
                                   List<String> skippedFiles, List<String> failedFiles,
                                   ModelInspectionResult inspection, String userMessage) {
        this(success, targetFolder, downloadedFiles, skippedFiles, failedFiles, inspection, userMessage, null);
    }

    public static XttsModelDownloadReport failed(Path targetFolder, String message, List<String> failedFiles) {
        return failed(targetFolder, message, failedFiles, null);
    }

    public static XttsModelDownloadReport failed(Path targetFolder, String message, List<String> failedFiles, Path diagnosticReport) {
        return new XttsModelDownloadReport(false, targetFolder, List.of(), List.of(), failedFiles, null, message, diagnosticReport);
    }

    public int completedFileCount() {
        return downloadedFiles.size() + skippedFiles.size();
    }
}
