package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** One ordered visual unit with an optional primary narration track. */
public record VideoTimelineItem(
        String id,
        List<TimelineVisualSource> visuals,
        Path narrationAudio,
        double durationSeconds,
        Map<String, String> metadata) {
    public VideoTimelineItem {
        id = id == null ? "" : id.strip();
        if (id.isBlank()) throw new IllegalArgumentException("timeline item id is required");
        visuals = visuals == null ? List.of() : List.copyOf(visuals);
        if (visuals.isEmpty()) throw new IllegalArgumentException("timeline item visuals are required");
        narrationAudio = narrationAudio == null ? null : narrationAudio.toAbsolutePath().normalize();
        durationSeconds = Math.max(0.05, durationSeconds);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
