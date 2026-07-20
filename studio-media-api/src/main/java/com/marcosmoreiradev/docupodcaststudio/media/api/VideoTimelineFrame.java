package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;

public record VideoTimelineFrame(Path image, Path audio, double durationSeconds, String label) {
    public VideoTimelineFrame {
        if (image == null) throw new IllegalArgumentException("frame image is required");
        durationSeconds = Math.max(0.05, durationSeconds);
        label = label == null ? "" : label.strip();
    }
}
