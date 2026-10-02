package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

public record TimelineVisualSource(
        TimelineVisualKind kind,
        Path file,
        double sourceStartSeconds,
        double durationSeconds,
        double fadeInSeconds,
        double fadeOutSeconds,
        String label,
        Map<String, String> metadata) {
    public TimelineVisualSource {
        kind = kind == null ? TimelineVisualKind.STILL_IMAGE : kind;
        if (file == null) throw new IllegalArgumentException("timeline visual file is required");
        file = file.toAbsolutePath().normalize();
        sourceStartSeconds = Math.max(0.0, sourceStartSeconds);
        durationSeconds = Math.max(0.05, durationSeconds);
        fadeInSeconds = Math.max(0.0, Math.min(durationSeconds, fadeInSeconds));
        fadeOutSeconds = Math.max(0.0, Math.min(durationSeconds, fadeOutSeconds));
        label = label == null ? "" : label.strip();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public TimelineVisualSource(TimelineVisualKind kind, Path file, double sourceStartSeconds,
                                double durationSeconds, String label, Map<String, String> metadata) {
        this(kind, file, sourceStartSeconds, durationSeconds, 0.0, 0.0, label, metadata);
    }
}
