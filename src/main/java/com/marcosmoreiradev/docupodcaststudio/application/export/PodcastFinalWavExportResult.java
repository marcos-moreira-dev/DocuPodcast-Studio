package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.nio.file.Path;
import java.util.List;

/** Result of exporting a deliverable podcast WAV. */
public record PodcastFinalWavExportResult(
        Path targetFile,
        Path reportFile,
        String jobId,
        boolean concatenatedFromSegments,
        int segmentCount,
        double durationSeconds,
        long sizeBytes,
        List<String> sourceClipPaths
) {
    public PodcastFinalWavExportResult {
        targetFile = targetFile == null ? Path.of("") : targetFile;
        reportFile = reportFile == null ? Path.of("") : reportFile;
        jobId = normalize(jobId);
        segmentCount = Math.max(0, segmentCount);
        durationSeconds = Math.max(0.0, durationSeconds);
        sizeBytes = Math.max(0L, sizeBytes);
        sourceClipPaths = sourceClipPaths == null ? List.of() : List.copyOf(sourceClipPaths);
    }

    public String modeLabel() {
        return concatenatedFromSegments ? "WAV final concatenado desde segmentos" : "WAV final existente copiado";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
