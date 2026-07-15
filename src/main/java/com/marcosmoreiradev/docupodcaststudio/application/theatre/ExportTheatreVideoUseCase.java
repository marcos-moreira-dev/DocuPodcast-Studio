package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.video.BuildTheatreCleanVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.BuildTheatreSpatialVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.RenderFinalVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.TheatreExportScope;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Theatre-only plan preparation and translation into the transversal video renderer. */
public final class ExportTheatreVideoUseCase {
    private final RenderFinalVideoPlanUseCase renderer;
    private final BuildTheatreCleanVideoPlanUseCase cleanPlanBuilder;
    private final BuildTheatreSpatialVideoPlanUseCase spatialPlanBuilder;
    private final BuildTheatreVideoAudioOverlayPlanUseCase overlayPlanBuilder;

    public ExportTheatreVideoUseCase(RenderFinalVideoPlanUseCase renderer) {
        this(renderer, new BuildTheatreCleanVideoPlanUseCase(), new BuildTheatreSpatialVideoPlanUseCase(),
                new BuildTheatreVideoAudioOverlayPlanUseCase());
    }

    ExportTheatreVideoUseCase(RenderFinalVideoPlanUseCase renderer,
                              BuildTheatreCleanVideoPlanUseCase cleanPlanBuilder,
                              BuildTheatreSpatialVideoPlanUseCase spatialPlanBuilder,
                              BuildTheatreVideoAudioOverlayPlanUseCase overlayPlanBuilder) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.cleanPlanBuilder = Objects.requireNonNull(cleanPlanBuilder, "cleanPlanBuilder");
        this.spatialPlanBuilder = Objects.requireNonNull(spatialPlanBuilder, "spatialPlanBuilder");
        this.overlayPlanBuilder = Objects.requireNonNull(overlayPlanBuilder, "overlayPlanBuilder");
    }

    public FinalVideoExportResult exportWork(FinalVideoExportRequest request,
                                             Consumer<VideoRenderProgress> progress,
                                             BooleanSupplier cancellationRequested) throws IOException {
        Consumer<VideoRenderProgress> safeProgress = progress == null ? ignored -> { } : progress;
        safeProgress.accept(VideoRenderProgress.preparing(0));
        SimpleVideoPlan plan = cleanPlanBuilder.build(request.project(), request.script(), request.storyboard(),
                request.jobs(), request.projectDirectory(), request.settings());
        return render(request, plan, safeProgress, cancellationRequested);
    }

    public FinalVideoExportResult exportSpatialMap(FinalVideoExportRequest request,
                                                   String spatialFrameMode,
                                                   TheatreExportScope scope,
                                                   Consumer<VideoRenderProgress> progress,
                                                   BooleanSupplier cancellationRequested) throws IOException {
        Consumer<VideoRenderProgress> safeProgress = progress == null ? ignored -> { } : progress;
        BooleanSupplier cancel = cancellationRequested == null ? () -> false : cancellationRequested;
        SimpleVideoPlan plan = spatialPlanBuilder.build(request.project(), request.script(), request.storyboard(),
                request.jobs(), request.projectDirectory(), request.settings(), spatialFrameMode,
                scope == null ? TheatreExportScope.all() : scope, safeProgress, cancel);
        return render(request, plan, safeProgress, cancel);
    }

    private FinalVideoExportResult render(FinalVideoExportRequest request,
                                          SimpleVideoPlan plan,
                                          Consumer<VideoRenderProgress> progress,
                                          BooleanSupplier cancellationRequested) throws IOException {
        VideoAudioOverlayPlan overlays = overlayPlanBuilder.build(
                request.project(), request.script(), plan, request.projectDirectory());
        FinalVideoRenderRequest renderRequest = FinalVideoRenderRequest.withoutOverlays(request, plan)
                .withAudioOverlayPlan(overlays);
        return renderer.render(renderRequest, progress, cancellationRequested);
    }
}
