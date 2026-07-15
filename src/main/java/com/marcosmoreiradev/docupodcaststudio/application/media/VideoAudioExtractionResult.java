package com.marcosmoreiradev.docupodcaststudio.application.media;

import java.nio.file.Path;

/** Result of extracting an audio track from a user-selected video. */
public record VideoAudioExtractionResult(
        Path sourceVideoFile,
        Path targetAudioFile,
        boolean successful,
        String message
) {
    public VideoAudioExtractionResult {
        message = message == null ? "" : message.strip();
    }
}
