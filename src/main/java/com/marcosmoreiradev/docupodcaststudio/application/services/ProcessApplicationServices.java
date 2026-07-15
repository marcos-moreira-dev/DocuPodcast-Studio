package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.process.InspectProcessJobContractUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.process.ListProcessJobsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.process.BuildProcessJobDashboardProjectionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SubmitVideoRenderJobUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.CancelVideoRenderJobUseCase;

/** Cross-engine long-running process use cases. */
public record ProcessApplicationServices(
        ListProcessJobsUseCase listProcessJobs,
        InspectProcessJobContractUseCase inspectProcessJobContract,
        BuildProcessJobDashboardProjectionUseCase buildProcessJobDashboardProjection,
        SubmitVideoRenderJobUseCase submitVideoRenderJob,
        CancelVideoRenderJobUseCase cancelVideoRenderJob
) {
    public ProcessApplicationServices(
            ListProcessJobsUseCase listProcessJobs,
            InspectProcessJobContractUseCase inspectProcessJobContract,
            SubmitVideoRenderJobUseCase submitVideoRenderJob,
            CancelVideoRenderJobUseCase cancelVideoRenderJob
    ) {
        this(listProcessJobs, inspectProcessJobContract, new BuildProcessJobDashboardProjectionUseCase(),
                submitVideoRenderJob, cancelVideoRenderJob);
    }
}
