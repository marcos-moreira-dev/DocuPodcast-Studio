package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.List;

/** Read-only operational diagnostic for the canonical PDF V2 workspace. */
public record PdfWorkspaceDiagnosticReport(
        boolean manifestValid,
        int declaredPages,
        int preparedPages,
        int temporaryFiles,
        int regions,
        int uncertainRegions,
        int derivedTreatments,
        long preparedBytes,
        long cacheBytes,
        int dependentAudioJobs,
        double ocrRegionRate,
        long totalPreparationMillis,
        long totalCpuMillis,
        long maximumObservedHeapBytes,
        double averagePreparationMillis,
        List<String> issues
) {
    public PdfWorkspaceDiagnosticReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
