package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.nio.file.Path;
import java.util.List;

/** Result of downloading the lightweight local voice runtime and default voice into the app folder. */
public record PiperRuntimeDownloadReport(
        boolean success,
        Path piperFolder,
        Path voiceFolder,
        PiperSetupReadinessReport readinessAfter,
        List<String> downloadedFiles,
        List<String> skippedFiles,
        List<String> failedFiles,
        String userMessage
) {
    public PiperRuntimeDownloadReport {
        downloadedFiles = List.copyOf(downloadedFiles == null ? List.of() : downloadedFiles);
        skippedFiles = List.copyOf(skippedFiles == null ? List.of() : skippedFiles);
        failedFiles = List.copyOf(failedFiles == null ? List.of() : failedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static PiperRuntimeDownloadReport failed(Path piperFolder, Path voiceFolder, String message, List<String> failedFiles) {
        return new PiperRuntimeDownloadReport(false, piperFolder, voiceFolder, null, List.of(), List.of(), failedFiles, message);
    }
}
