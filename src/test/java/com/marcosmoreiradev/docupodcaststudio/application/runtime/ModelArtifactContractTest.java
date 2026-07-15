package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModelArtifactContractTest {
    @Test
    void xttsContractReportsMissingRequiredFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("docupodcast-model-contract");
        Files.writeString(tempDir.resolve("config.json"), "{}");
        RuntimeArtifactInspection inspection = ModelArtifactContract.xttsAdvancedVoice().inspect(tempDir);

        assertTrue(inspection.blocked());
        assertTrue(inspection.missingRequired().stream().anyMatch(item -> item.contains("model.pth")));
        assertTrue(inspection.userMessage().contains("Voz IA avanzada"));
    }

    @Test
    void ffmpegContractAcceptsFfmpegAndKeepsFfprobeOptional() throws Exception {
        Path tempDir = Files.createTempDirectory("docupodcast-ffmpeg-contract");
        Files.writeString(tempDir.resolve("ffmpeg.exe"), "bin");
        RuntimeArtifactInspection inspection = ModelArtifactContract.ffmpegVideoAudio().inspect(tempDir);

        assertFalse(inspection.blocked());
        assertTrue(inspection.missingOptional().stream().anyMatch(item -> item.contains("ffprobe")));
    }
}
