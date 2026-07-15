package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoRenderJobSnapshotTest {
    @Test
    void queuedJobIsPersistentAndCancelable() {
        SimpleVideoPlan plan = new SimpleVideoPlan("Demo", java.util.List.of(), 5.0, java.time.Instant.now());
        VideoRenderCommandPlan commandPlan = new VideoRenderCommandPlan(
                plan,
                SimpleVideoExportSettings.defaults(),
                new FfmpegToolDiscovery(Path.of("tools/ffmpeg/bin/ffmpeg.exe"), Path.of("tools/ffmpeg/bin/ffprobe.exe"), true, true, "ok"),
                "video-simple.mp4",
                java.util.List.of("ffmpeg ..."),
                java.util.List.of(),
                java.time.Instant.now());
        SimpleVideoPackageExportResult export = new SimpleVideoPackageExportResult(
                Path.of("demo/video"), Path.of("plan.md"), Path.of("frames.csv"), Path.of("concat.txt"),
                Path.of("render.bat"), Path.of("manifest.json"), Path.of("commands.txt"), Path.of("state.md"),
                "PACKAGE_NEEDS_REVIEW", false, "video-simple.mp4", 0, 0, 0, 0);

        VideoRenderJobSnapshot job = VideoRenderJobSnapshot.queued("video-001", export, commandPlan, "exports/video", "jobs/video/video-001");

        assertTrue(job.state() == ProcessJobState.QUEUED);
        assertTrue(job.cancellable());
        assertTrue(job.logs().stdoutLogPath().contains("stdout.log"));
        assertTrue(job.cancelled("prueba").state() == ProcessJobState.CANCELLED);
    }
}
