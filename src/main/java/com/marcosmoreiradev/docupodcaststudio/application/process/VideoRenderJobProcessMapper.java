package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;

import java.util.List;
import java.util.Objects;

/** Maps persistent FFmpeg render jobs into the common process-job contract. */
public final class VideoRenderJobProcessMapper {
    public ProcessJobSnapshot map(VideoRenderJobSnapshot job) {
        Objects.requireNonNull(job, "job");
        return new ProcessJobSnapshot(
                job.jobId(),
                ProcessJobKind.VIDEO_RENDER,
                "Render de video: " + job.jobId(),
                job.state(),
                job.stage(),
                job.progress(),
                job.totalFrames() <= 0 ? "" : Integer.toString(job.completedFrames()),
                job.currentStep(),
                0L,
                job.message(),
                job.jobRelativeDirectory(),
                job.cancellable(),
                job.recoverable(),
                job.artifacts(),
                job.logs(),
                job.createdAt(),
                job.updatedAt()
        );
    }

    public List<ProcessJobSnapshot> mapAll(List<VideoRenderJobSnapshot> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }
        return jobs.stream().map(this::map).toList();
    }
}
