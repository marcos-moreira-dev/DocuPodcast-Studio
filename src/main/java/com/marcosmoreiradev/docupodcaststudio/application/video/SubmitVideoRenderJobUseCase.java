package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Creates a persistent, cancelable FFmpeg render job for an already exported storyboard/video package. */
public final class SubmitVideoRenderJobUseCase {
    private final VideoRenderJobRepository repository;

    public SubmitVideoRenderJobUseCase(VideoRenderJobRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public VideoRenderJobSnapshot submit(Path projectDirectory,
                                         SimpleVideoPackageExportResult packageExport,
                                         VideoRenderCommandPlan commandPlan) throws IOException {
        Objects.requireNonNull(projectDirectory, "projectDirectory");
        Objects.requireNonNull(packageExport, "packageExport");
        Objects.requireNonNull(commandPlan, "commandPlan");
        Path root = projectDirectory.toAbsolutePath().normalize();
        String jobId = "video-" + Instant.now().toString().replaceAll("[^0-9A-Za-z]", "").toLowerCase(Locale.ROOT);
        String packageRelative = relative(root, packageExport.rootDirectory());
        String jobRelative = "jobs/video/" + jobId;
        VideoRenderJobSnapshot snapshot = VideoRenderJobSnapshot.queued(jobId, packageExport, commandPlan, packageRelative, jobRelative);
        repository.save(root, snapshot, commandPlan);
        return snapshot;
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
