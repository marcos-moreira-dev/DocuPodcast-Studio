package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EnginePresetId;

import java.nio.file.Path;
import java.util.Map;

/** Neutral application artifact used while committing a generated narrative clip. */
public record NarrativeGeneratedVideoArtifact(
        Path clipPath,
        Path continuationFramePath,
        int width,
        int height,
        int framesPerSecond,
        double durationSeconds,
        EngineId engineId,
        EnginePresetId presetId,
        long seed,
        Map<String, String> metadata) {
    public NarrativeGeneratedVideoArtifact {
        if (clipPath == null || continuationFramePath == null) {
            throw new IllegalArgumentException("clip and continuation frame are required");
        }
        clipPath = clipPath.toAbsolutePath().normalize();
        continuationFramePath = continuationFramePath.toAbsolutePath().normalize();
        width = Math.max(1, width);
        height = Math.max(1, height);
        framesPerSecond = Math.max(1, framesPerSecond);
        durationSeconds = Math.max(0.05, durationSeconds);
        if (engineId == null || presetId == null) throw new IllegalArgumentException("engine and preset are required");
        seed = Math.max(0L, seed);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
