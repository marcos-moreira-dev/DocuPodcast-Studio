package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FfmpegRuntimeReportTest {

    @Test
    void automaticPolicyPrefersAnAvailableGpuEncoder() {
        FfmpegRuntimeReport report = report(List.of("libx264", "h264_nvenc"));

        assertEquals("h264_nvenc", report.effectiveEncoder(VideoEncoderPolicy.AUTO));
    }

    @Test
    void automaticPolicyFallsBackToCpuAndExplicitUnavailableEncoderIsNotPromised() {
        FfmpegRuntimeReport report = report(List.of("libx264"));

        assertEquals("libx264", report.effectiveEncoder(VideoEncoderPolicy.AUTO));
        assertEquals("", report.effectiveEncoder(VideoEncoderPolicy.NVIDIA_NVENC));
    }

    private static FfmpegRuntimeReport report(List<String> encoders) {
        return new FfmpegRuntimeReport(
                Path.of("ffmpeg.exe"),
                Path.of("ffprobe.exe"),
                true,
                true,
                "test",
                "test",
                encoders,
                List.of()
        );
    }
}
