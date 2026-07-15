package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegAutocontainedT116SourceTest {
    @Test
    void runtimeLayoutRequiresFfmpegAndFfprobeForFinalProduct() throws Exception {
        String runtime = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/BuildRuntimeBundleManifestUseCase.java");
        String script = read("scripts/29-verificar-runtime-layout.bat");
        String preflight = read("scripts/tts/preflight-piper-ffmpeg.ps1");

        assertTrue(runtime.contains("tools/ffmpeg/bin/ffmpeg.exe"));
        assertTrue(runtime.contains("tools/ffmpeg/bin/ffprobe.exe"));
        assertTrue(runtime.contains("FFprobe local obligatorio"));
        assertTrue(script.contains("tools\\ffmpeg\\bin\\ffmpeg.exe"));
        assertTrue(script.contains("tools\\ffmpeg\\bin\\ffprobe.exe"));
        assertTrue(preflight.contains("ffmpeg -encoders") || preflight.contains("-encoders"));
        assertTrue(preflight.contains("libx264"));
        assertTrue(preflight.contains("h264_nvenc"));
        assertTrue(preflight.contains("h264_qsv"));
        assertTrue(preflight.contains("h264_amf"));
    }

    @Test
    void applicationHasRuntimeProbeForEncoderSupport() throws Exception {
        String probe = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/FfmpegRuntimeProbeUseCase.java");
        String report = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/FfmpegRuntimeReport.java");

        assertTrue(probe.contains("ffmpeg -encoders") || probe.contains("-encoders"));
        assertTrue(probe.contains("ffprobe.exe"));
        assertTrue(report.contains("supportsNvenc"));
        assertTrue(report.contains("supportsQsv"));
        assertTrue(report.contains("supportsAmf"));
        assertTrue(report.contains("readyForFinalVideo"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
