package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegRuntimeProbeUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesExpectedVideoEncodersFromFfmpegOutput() {
        String output = " V....D libx264 libx264 H.264\n"
                + " V..... h264_nvenc NVIDIA NVENC H.264 encoder\n"
                + " V..... h264_qsv Intel QSV H.264 encoder\n"
                + " V..... h264_amf AMD AMF H.264 encoder\n";

        List<String> encoders = FfmpegRuntimeProbeUseCase.parseEncoderOutput(output);

        assertTrue(encoders.contains("libx264"));
        assertTrue(encoders.contains("h264_nvenc"));
        assertTrue(encoders.contains("h264_qsv"));
        assertTrue(encoders.contains("h264_amf"));
    }

    @Test
    void recognizesExpectedEncodersFromBuildConfigurationFallback() {
        String output = "configuration: --enable-libx264 --enable-nvenc --enable-libvpl --enable-amf";

        List<String> encoders = FfmpegRuntimeProbeUseCase.parseConfigurationOutput(output);

        assertTrue(encoders.contains("libx264"));
        assertTrue(encoders.contains("h264_nvenc"));
        assertTrue(encoders.contains("h264_qsv"));
        assertTrue(encoders.contains("h264_amf"));
    }

    @Test
    void reportRequiresFfmpegFfprobeAndCpuEncoderForFinalVideo() {
        FfmpegRuntimeReport report = new FfmpegRuntimeReport(null, null, true, false,
                "ffmpeg version", "", List.of("libx264"), List.of("Falta ffprobe"));

        assertFalse(report.readyForFinalVideo());
        assertTrue(report.supportsPolicy(VideoEncoderPolicy.CPU_X264));
        assertFalse(report.supportsPolicy(VideoEncoderPolicy.NVIDIA_NVENC));
    }

    @Test
    void inspectUsesInjectedExternalProcessRunner() throws Exception {
        Path ffmpeg = tempDir.resolve("ffmpeg.exe");
        Path ffprobe = tempDir.resolve("ffprobe.exe");
        Files.writeString(ffmpeg, "fake");
        Files.writeString(ffprobe, "fake");
        FakeRunner runner = new FakeRunner();

        FfmpegRuntimeReport report = new FfmpegRuntimeProbeUseCase(runner)
                .inspect(new FfmpegToolDiscovery(ffmpeg, ffprobe, true, true, "fake"));

        assertTrue(report.readyForFinalVideo(), report.warnings().toString());
        assertTrue(report.encoders().contains("libx264"));
        assertTrue(report.encoders().contains("h264_nvenc"));
        assertTrue(runner.commands.stream().anyMatch(command -> command.contains("-encoders")));
    }

    private static final class FakeRunner implements ExternalProcessRunner {
        private final ArrayList<List<String>> commands = new ArrayList<>();

        @Override
        public ExternalProcessResult run(ExternalProcessRequest request) {
            commands.add(request.command());
            String output;
            if (request.command().contains("-encoders")) {
                output = " V....D libx264 libx264 H.264\n V..... h264_nvenc NVIDIA NVENC H.264 encoder";
            } else if (request.command().get(0).toLowerCase().contains("ffprobe")) {
                output = "ffprobe version fake";
            } else {
                output = "ffmpeg version fake --enable-libx264 --enable-nvenc";
            }
            return new ExternalProcessResult(0, false, false, output, "",
                    request.commandAudit(), Duration.ofMillis(1));
        }
    }
}
