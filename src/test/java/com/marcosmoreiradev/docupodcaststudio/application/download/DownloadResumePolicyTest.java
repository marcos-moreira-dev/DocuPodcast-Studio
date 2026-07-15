package com.marcosmoreiradev.docupodcaststudio.application.download;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DownloadResumePolicyTest {
    @Test
    void resumesOnlyWhenAllowedAndPartialFileExists() throws Exception {
        Path tempDir = Files.createTempDirectory("docupodcast-download-policy");
        Path partial = tempDir.resolve("runtime.zip");
        Files.writeString(partial, "partial");
        DownloadRequest resumable = new DownloadRequest("ffmpeg", "https://example.test/ffmpeg.zip", partial, true, Map.of());
        DownloadRequest notResumable = new DownloadRequest("ffmpeg", "https://example.test/ffmpeg.zip", partial, false, Map.of());

        DownloadResumePolicy policy = new DownloadResumePolicy();
        assertTrue(policy.canResume(resumable));
        assertFalse(policy.canResume(notResumable));
    }
}
