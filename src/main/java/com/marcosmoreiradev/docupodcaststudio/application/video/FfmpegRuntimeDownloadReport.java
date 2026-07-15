package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;
import java.util.List;

/** Result of preparing the local video runtime from the configured download URL. */
public record FfmpegRuntimeDownloadReport(
        boolean success,
        Path downloadFile,
        Path extractedFolder,
        Path targetFolder,
        Path ffmpegExecutable,
        Path ffprobeExecutable,
        FfmpegRuntimeReport runtimeAfter,
        List<String> downloadedFiles,
        List<String> copiedFiles,
        List<String> failedItems,
        String userMessage
) {
    public FfmpegRuntimeDownloadReport {
        downloadedFiles = List.copyOf(downloadedFiles == null ? List.of() : downloadedFiles);
        copiedFiles = List.copyOf(copiedFiles == null ? List.of() : copiedFiles);
        failedItems = List.copyOf(failedItems == null ? List.of() : failedItems);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static FfmpegRuntimeDownloadReport failed(Path downloadFile, Path extractedFolder, Path targetFolder,
                                                     String message, List<String> failedItems) {
        return new FfmpegRuntimeDownloadReport(false, downloadFile, extractedFolder, targetFolder,
                null, null, null, List.of(), List.of(), failedItems, message);
    }
}
