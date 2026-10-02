package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildVideoRenderCommandPlanUseCaseTest {
    @Test
    void marksPackageReadyWhenAudioImagesAndFfmpegAreAvailable() {
        SimpleVideoFrame frame = new SimpleVideoFrame(
                "FRAME-001", "SEG-001", "Intro", "Texto", "IMG-001", "media/images/intro.png",
                "jobs/JOB-001/audio/SEG-001.wav", 2.0, 1.0, true, true);
        SimpleVideoPlan plan = new SimpleVideoPlan("Demo", List.of(frame), 1.0, Instant.now());
        FfmpegToolDiscovery ffmpeg = new FfmpegToolDiscovery(Path.of("tools/ffmpeg/bin/ffmpeg.exe"), null,
                true, true, "FFmpeg embebido listo");

        VideoRenderCommandPlan commandPlan = new BuildVideoRenderCommandPlanUseCase()
                .build(plan, SimpleVideoExportSettings.defaults(), ffmpeg);

        assertTrue(commandPlan.renderableAsMp4());
        assertTrue(commandPlan.commandsText().contains("docupodcast-simple-video-render-v1"));
        assertTrue(commandPlan.commandsText().contains("video-simple.mp4"));
        assertTrue(commandPlan.manifestJson().contains("MP4_RENDER_READY"));
    }

    @Test
    void keepsPackageAuditableWhenFfmpegOrAssetsAreMissing() {
        SimpleVideoFrame frame = new SimpleVideoFrame(
                "FRAME-001", "SEG-001", "Intro", "Texto", "", "", "", 0.0, 1.0, false, false);
        SimpleVideoPlan plan = new SimpleVideoPlan("Demo", List.of(frame), 1.0, Instant.now());
        FfmpegToolDiscovery ffmpeg = new FfmpegToolDiscovery(Path.of("tools/ffmpeg/bin/ffmpeg.exe"), null,
                true, false, "No listo");

        VideoRenderCommandPlan commandPlan = new BuildVideoRenderCommandPlanUseCase()
                .build(plan, SimpleVideoExportSettings.defaults(), ffmpeg);

        assertFalse(commandPlan.renderableAsMp4());
        assertTrue(commandPlan.manifestJson().contains("PACKAGE_NEEDS_REVIEW"));
        assertTrue(commandPlan.warnings().stream().anyMatch(warning -> warning.contains("FFmpeg")));
        assertTrue(commandPlan.commandsText().contains("requiere revisión"));
    }

    @Test
    void recordsComputePolicyAndRequestedHardwareEncoderInManifest() {
        SimpleVideoFrame frame = new SimpleVideoFrame(
                "FRAME-001", "SEG-001", "Intro", "Texto", "IMG-001", "media/images/intro.png",
                "jobs/JOB-001/audio/SEG-001.wav", 2.0, 1.0, true, true);
        SimpleVideoPlan plan = new SimpleVideoPlan("Demo", List.of(frame), 1.0, Instant.now());
        SimpleVideoExportSettings settings = new SimpleVideoExportSettings(
                SimpleVideoResolutionPreset.QHD_2K, 30, 1.0, true, true,
                ComputeDevicePolicy.PREFER_GPU, VideoEncoderPolicy.NVIDIA_NVENC);
        FfmpegToolDiscovery ffmpeg = new FfmpegToolDiscovery(Path.of("ffmpeg.exe"), null, false, true, "externo");

        VideoRenderCommandPlan commandPlan = new BuildVideoRenderCommandPlanUseCase()
                .build(plan, settings, ffmpeg);

        assertTrue(commandPlan.manifestJson().contains("\"renderDevicePolicy\": \"PREFER_GPU\""));
        assertTrue(commandPlan.manifestJson().contains("\"requestedEncoder\": \"NVIDIA_NVENC\""));
        assertTrue(commandPlan.commandsText().contains("h264_nvenc"));
        assertTrue(commandPlan.warnings().stream().anyMatch(warning -> warning.contains("Encoder por hardware")));
    }
}
