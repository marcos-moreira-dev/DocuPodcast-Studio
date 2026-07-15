package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoExportRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoExportResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.FinalVideoRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.video.RenderFinalVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.application.video.VideoAudioOverlayPlan;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Documentary-specific export entry point; it never consults theatre state or overlays. */
public final class ExportDocumentStudyVideoUseCase {
    private final RenderFinalVideoPlanUseCase renderer;

    public ExportDocumentStudyVideoUseCase(RenderFinalVideoPlanUseCase renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public FinalVideoExportResult export(FinalVideoExportRequest request,
                                         SimpleVideoPlan plan,
                                         Consumer<VideoRenderProgress> progress,
                                         BooleanSupplier cancellationRequested) throws IOException {
        return export(request, plan, VideoAudioOverlayPlan.emptyPlan(), progress, cancellationRequested);
    }

    public FinalVideoExportResult export(FinalVideoExportRequest request,
                                         SimpleVideoPlan plan,
                                         VideoAudioOverlayPlan overlays,
                                         Consumer<VideoRenderProgress> progress,
                                         BooleanSupplier cancellationRequested) throws IOException {
        FinalVideoRenderRequest renderRequest = FinalVideoRenderRequest.withoutOverlays(request, plan)
                .withAudioOverlayPlan(overlays);
        return renderer.render(renderRequest, progress, cancellationRequested);
    }
}
