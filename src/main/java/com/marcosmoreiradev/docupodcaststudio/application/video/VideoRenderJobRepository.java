package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Persists FFmpeg render jobs under the project container. */
public interface VideoRenderJobRepository {
    void save(Path projectDirectory, VideoRenderJobSnapshot snapshot, VideoRenderCommandPlan commandPlan) throws IOException;

    Optional<VideoRenderJobSnapshot> load(Path projectDirectory, String jobId) throws IOException;

    List<VideoRenderJobSnapshot> list(Path projectDirectory) throws IOException;
}
