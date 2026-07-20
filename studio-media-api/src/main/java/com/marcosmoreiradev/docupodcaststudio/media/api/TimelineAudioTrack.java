package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

public record TimelineAudioTrack(
        String id,
        Path file,
        double sourceStartSeconds,
        double sourceEndSeconds,
        double timelineStartSeconds,
        double volume,
        double fadeInSeconds,
        double fadeOutSeconds,
        Map<String, String> metadata) {
    public TimelineAudioTrack {
        id = id == null ? "" : id.strip();
        if (file == null) throw new IllegalArgumentException("timeline audio file is required");
        file = file.toAbsolutePath().normalize();
        sourceStartSeconds = Math.max(0.0, sourceStartSeconds);
        sourceEndSeconds = Math.max(sourceStartSeconds, sourceEndSeconds);
        timelineStartSeconds = Math.max(0.0, timelineStartSeconds);
        volume = Double.isFinite(volume) ? Math.max(0.0, Math.min(2.0, volume)) : 1.0;
        fadeInSeconds = Math.max(0.0, fadeInSeconds);
        fadeOutSeconds = Math.max(0.0, fadeOutSeconds);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
