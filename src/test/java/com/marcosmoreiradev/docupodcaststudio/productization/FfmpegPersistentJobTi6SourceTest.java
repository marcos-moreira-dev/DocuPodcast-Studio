package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FfmpegPersistentJobTi6SourceTest {
    @Test
    void ti6DocumentsFfmpegPersistentCancelableJob() throws Exception {
        String snapshot = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/VideoRenderJobSnapshot.java"));
        String repository = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/video/VideoRenderJobFileRepository.java"));
        String processKind = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/process/ProcessJobKind.java"));
        String docs = Files.readString(Path.of("docs/productizacion/TI6_FFMPEG_JOB_PERSISTENTE_CANCELABLE.md"));

        assertTrue(snapshot.contains("cancellationRequested"));
        assertTrue(repository.contains("video-render-job.json"));
        assertTrue(repository.contains("cancel.requested"));
        assertTrue(processKind.contains("VIDEO_RENDER(\"Render de video\", true)"));
        assertTrue(docs.contains("FFmpeg como job persistente/cancelable"));
    }
}
