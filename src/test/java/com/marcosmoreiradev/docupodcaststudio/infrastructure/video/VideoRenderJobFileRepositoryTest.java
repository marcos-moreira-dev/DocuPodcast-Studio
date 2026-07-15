package com.marcosmoreiradev.docupodcaststudio.infrastructure.video;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoExportSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderCommandPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class VideoRenderJobFileRepositoryTest {
    @Test
    void persistsAndCancelsVideoRenderJob() throws Exception {
        Path tempDir = Files.createTempDirectory("docupodcast-video-job-test");
        VideoRenderJobFileRepository repository = new VideoRenderJobFileRepository();
        VideoRenderCommandPlan commandPlan = new VideoRenderCommandPlan(
                new SimpleVideoPlan("Demo", java.util.List.of(), 5.0, java.time.Instant.now()),
                SimpleVideoExportSettings.defaults(), null, "video-simple.mp4", java.util.List.of("ffmpeg"), java.util.List.of(), java.time.Instant.now());
        VideoRenderJobSnapshot job = VideoRenderJobSnapshot.queued("video-001", null, commandPlan, "exports/video", "jobs/video/video-001");

        repository.save(tempDir, job, commandPlan);
        VideoRenderJobSnapshot loaded = repository.load(tempDir, "video-001").orElseThrow();
        repository.save(tempDir, loaded.cancelled("cancelado"), null);

        assertTrue(repository.load(tempDir, "video-001").orElseThrow().state() == ProcessJobState.CANCELLED);
        assertTrue(Files.isRegularFile(tempDir.resolve("jobs/video/video-001/video-render-job.json")));
        assertTrue(Files.isRegularFile(tempDir.resolve("jobs/video/video-001/cancel.requested")));
    }
}
