package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.nio.file.Path;
import java.util.Objects;

/** Result of downloading/exporting a voice sample to a user-selected folder. */
public record VoiceSampleDownloadResult(
        Path copiedFile,
        String message
) {
    public VoiceSampleDownloadResult {
        copiedFile = Objects.requireNonNull(copiedFile, "copiedFile");
        message = message == null ? "" : message.strip();
    }
}
