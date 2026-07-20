package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Deterministic composition request; it is deliberately not generative video. */
public record VideoRenderRequest(
        List<VideoTimelineFrame> timeline,
        int width,
        int height,
        int framesPerSecond,
        Path outputFile,
        Map<String, String> options) {
    public VideoRenderRequest {
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        width = Math.max(320, width);
        height = Math.max(180, height);
        framesPerSecond = Math.max(1, Math.min(120, framesPerSecond));
        options = options == null ? Map.of() : Map.copyOf(options);
        if (timeline.isEmpty()) throw new IllegalArgumentException("video timeline is required");
        if (outputFile == null) throw new IllegalArgumentException("video output file is required");
    }
}
