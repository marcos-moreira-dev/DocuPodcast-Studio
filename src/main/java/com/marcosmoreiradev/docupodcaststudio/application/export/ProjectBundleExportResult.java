package com.marcosmoreiradev.docupodcaststudio.application.export;

import java.nio.file.Path;
import java.util.List;

/** Result of exporting a portable project bundle. */
public record ProjectBundleExportResult(
        Path rootDirectory,
        Path manifestFile,
        Path readmeFile,
        Path fileIndexFile,
        Path exportReadinessFile,
        Path voiceReferenceSamplesReportFile,
        Path voiceReferenceSamplesIndexFile,
        int copiedInputs,
        int copiedOutputs,
        int copiedJobs,
        int copiedAssets,
        int voiceReferenceSampleCount,
        List<BundleArtifactMetadata> artifacts
) {
    public ProjectBundleExportResult {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
