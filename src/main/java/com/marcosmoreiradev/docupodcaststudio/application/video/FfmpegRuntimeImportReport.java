package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;
import java.util.List;

/** Result of importing FFmpeg/FFprobe into the application-controlled runtime layout. */
public record FfmpegRuntimeImportReport(
        boolean success,
        Path sourceFolder,
        Path targetBinFolder,
        Path ffmpegExecutable,
        Path ffprobeExecutable,
        List<String> copiedFiles,
        String userMessage
) {
    public FfmpegRuntimeImportReport {
        copiedFiles = List.copyOf(copiedFiles == null ? List.of() : copiedFiles);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static FfmpegRuntimeImportReport failed(Path sourceFolder, Path targetBinFolder, String userMessage) {
        return new FfmpegRuntimeImportReport(false, sourceFolder, targetBinFolder, null, null, List.of(), userMessage);
    }
}
