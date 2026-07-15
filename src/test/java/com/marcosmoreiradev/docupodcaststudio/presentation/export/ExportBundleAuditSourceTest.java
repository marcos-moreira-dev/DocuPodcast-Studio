package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportBundleAuditSourceTest {
    @Test
    void projectBundleExporterWritesAuditableReportsAndHashes() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/export/FileSystemProjectBundleExporter.java"));
        assertTrue(source.contains("BUNDLE_FILE_INDEX.tsv"));
        assertTrue(source.contains("MANIFEST_EXPORTACION.md"));
        assertTrue(source.contains("README_EXPORTACION.md"));
        assertTrue(source.contains("BundleArtifactMetadata"));
        assertTrue(source.contains("SHA-256"));
        assertTrue(source.contains("MessageDigest.getInstance"));
        assertTrue(source.contains("copyRegisteredAssets"));
        assertTrue(source.contains("collectArtifacts"));
        assertTrue(source.contains("fileIndex"));
        assertTrue(source.contains("EXPORT_READINESS.md"));
        assertTrue(source.contains("VOICE_REFERENCE_SAMPLES.md"));
        assertTrue(source.contains("BuildVoiceReferenceSamplesExportReportUseCase"));
        assertTrue(source.contains("InspectExportReadinessUseCase"));
    }

    @Test
    void projectBundleExportResultExposesAuditCounters() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/export/ProjectBundleExportResult.java"));
        assertTrue(source.contains("Path fileIndexFile"));
        assertTrue(source.contains("Path exportReadinessFile"));
        assertTrue(source.contains("Path voiceReferenceSamplesReportFile"));
        assertTrue(source.contains("Path voiceReferenceSamplesIndexFile"));
        assertTrue(source.contains("int copiedInputs"));
        assertTrue(source.contains("int copiedOutputs"));
        assertTrue(source.contains("int copiedJobs"));
        assertTrue(source.contains("int copiedAssets"));
        assertTrue(source.contains("int voiceReferenceSampleCount"));
        assertTrue(source.contains("List<BundleArtifactMetadata> artifacts"));
    }
}
