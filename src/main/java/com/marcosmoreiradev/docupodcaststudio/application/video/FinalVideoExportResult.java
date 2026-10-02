package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;

import java.nio.file.Path;
import java.util.List;

/** Result of rendering a final MP4 file from the DocuPodcast visual sequence. */
public record FinalVideoExportResult(
        Path targetFile,
        Path workDirectory,
        SimpleVideoResolutionPreset resolution,
        int frameCount,
        double totalDurationSeconds,
        long bytes,
        List<String> warnings
) {
    public FinalVideoExportResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public String humanSummary() {
        return "Video MP4 exportado: " + targetFile
                + " · calidad " + (resolution == null ? "personalizada" : resolution.label())
                + " · frames " + frameCount
                + " · duración estimada " + String.format(java.util.Locale.ROOT, "%.1f", totalDurationSeconds) + " s";
    }
}
