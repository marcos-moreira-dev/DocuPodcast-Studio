package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;

/** Result of finding local FFmpeg tools without requiring a system PATH modification. */
public record FfmpegToolDiscovery(
        Path ffmpegExecutable,
        Path ffprobeExecutable,
        boolean embedded,
        boolean ready,
        String message
) {
    public FfmpegToolDiscovery {
        message = message == null ? "" : message.strip();
    }
}
