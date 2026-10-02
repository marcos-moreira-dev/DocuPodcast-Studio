package com.marcosmoreiradev.docupodcaststudio.infrastructure.observability;

import com.marcosmoreiradev.docupodcaststudio.application.observability.SupportBundleExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSupportBundleExporterTest {
    @TempDir Path temp;

    @Test void exportsOnlySanitizedTextDiagnostics() throws Exception {
        Path logs = Files.createDirectories(temp.resolve("logs"));
        Path project = Files.createDirectories(temp.resolve("project"));
        Files.writeString(logs.resolve("studio.log"),
                "authorization=secret prompt=private " + project, StandardCharsets.UTF_8);
        Path manifest = project.resolve("job.json");
        Files.writeString(manifest, "{\"engineId\":\"fake\",\"prompt\":\"private\"}");
        Path binary = project.resolve("artifact.png");
        Files.write(binary, new byte[]{1, 2, 3});

        SupportBundleExporter.ExportResult result = new FileSupportBundleExporter().export(
                new SupportBundleExporter.ExportRequest(temp.resolve("support"), logs, project,
                        List.of(manifest, binary), Map.of("projectPath", project.toString())));

        assertTrue(Files.isRegularFile(result.archive()));
        assertTrue(result.omittedEntries().contains("artifact.png"));
        try (ZipFile zip = new ZipFile(result.archive().toFile(), StandardCharsets.UTF_8)) {
            assertNotNull(zip.getEntry("diagnostics.json"));
            assertNotNull(zip.getEntry("logs/studio.log"));
            assertNotNull(zip.getEntry("manifests/01-job.json"));
            assertFalse(zip.stream().anyMatch(entry -> entry.getName().endsWith("artifact.png")));
            String log = new String(zip.getInputStream(zip.getEntry("logs/studio.log")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertFalse(log.contains("secret"));
            assertFalse(log.contains("private"));
            assertTrue(log.contains("<redacted>"));
        }
    }
}
