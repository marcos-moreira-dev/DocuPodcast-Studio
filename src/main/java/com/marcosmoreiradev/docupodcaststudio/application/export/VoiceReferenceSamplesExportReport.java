package com.marcosmoreiradev.docupodcaststudio.application.export;

/** Auditable report for voice reference samples included in a project bundle. */
public record VoiceReferenceSamplesExportReport(
        int voiceCount,
        int sampleSetCount,
        int sampleCount,
        int registeredAssetCount,
        int missingFileCount,
        int generatedTestFileCount,
        String markdown,
        String tsv
) {
    public VoiceReferenceSamplesExportReport {
        voiceCount = Math.max(0, voiceCount);
        sampleSetCount = Math.max(0, sampleSetCount);
        sampleCount = Math.max(0, sampleCount);
        registeredAssetCount = Math.max(0, registeredAssetCount);
        missingFileCount = Math.max(0, missingFileCount);
        generatedTestFileCount = Math.max(0, generatedTestFileCount);
        markdown = markdown == null ? "" : markdown;
        tsv = tsv == null ? "" : tsv;
    }

    public boolean hasReferenceSamples() {
        return sampleCount > 0;
    }

    public boolean hasMissingFiles() {
        return missingFileCount > 0;
    }
}
