package com.marcosmoreiradev.docupodcaststudio.application.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegRuntimeDownloadReportTest {
    @Test
    void normalizesCollectionsAndMessage() {
        FfmpegRuntimeDownloadReport report = new FfmpegRuntimeDownloadReport(
                true,
                Path.of("runtime.zip"),
                Path.of("extracted"),
                Path.of("tools/ffmpeg/bin"),
                Path.of("tools/ffmpeg/bin/ffmpeg.exe"),
                Path.of("tools/ffmpeg/bin/ffprobe.exe"),
                null,
                List.of("zip"),
                List.of("bin/ffmpeg.exe", "bin/ffprobe.exe"),
                null,
                "  listo  ");

        assertTrue(report.success());
        assertEquals("listo", report.userMessage());
        assertEquals(List.of(), report.failedItems());
        assertEquals(2, report.copiedFiles().size());
    }

    @Test
    void failedFactoryKeepsTroubleshootingItems() {
        FfmpegRuntimeDownloadReport report = FfmpegRuntimeDownloadReport.failed(
                Path.of("runtime.zip"),
                Path.of("extracted"),
                Path.of("tools/ffmpeg/bin"),
                "falló",
                List.of("HTTP 404"));

        assertFalse(report.success());
        assertEquals(List.of("HTTP 404"), report.failedItems());
        assertEquals("falló", report.userMessage());
    }
}
