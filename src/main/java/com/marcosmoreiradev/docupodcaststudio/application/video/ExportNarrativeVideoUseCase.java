package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Prepares the standard narrative-video plan and delegates encoding to the transversal renderer. */
public final class ExportNarrativeVideoUseCase {
    private final BuildSimpleVideoPlanUseCase buildSimpleVideoPlan;
    private final BuildNarrativeVideoPlanUseCase buildNarrativeVideoPlan;
    private final RenderFinalVideoPlanUseCase renderer;

    public ExportNarrativeVideoUseCase() {
        this(new BuildSimpleVideoPlanUseCase(),
                new FfmpegRuntimeProbeUseCase(ExternalProcessRunner.unavailable("ExportNarrativeVideoUseCase")),
                new EmbeddedFfmpegLocator(),
                ExternalProcessRunner.unavailable("ExportNarrativeVideoUseCase"));
    }

    public ExportNarrativeVideoUseCase(BuildSimpleVideoPlanUseCase buildSimpleVideoPlan,
                                       FfmpegRuntimeProbeUseCase probe,
                                       EmbeddedFfmpegLocator locator) {
        this(buildSimpleVideoPlan, probe, locator, ExternalProcessRunner.unavailable("ExportNarrativeVideoUseCase"));
    }

    public ExportNarrativeVideoUseCase(BuildSimpleVideoPlanUseCase buildSimpleVideoPlan,
                                       FfmpegRuntimeProbeUseCase probe,
                                       EmbeddedFfmpegLocator locator,
                                       ExternalProcessRunner runner) {
        this(buildSimpleVideoPlan, new RenderFinalVideoPlanUseCase(probe, locator, runner));
    }

    public ExportNarrativeVideoUseCase(BuildSimpleVideoPlanUseCase buildSimpleVideoPlan,
                                       RenderFinalVideoPlanUseCase renderer) {
        this.buildSimpleVideoPlan = Objects.requireNonNull(buildSimpleVideoPlan, "buildSimpleVideoPlan");
        this.buildNarrativeVideoPlan = new BuildNarrativeVideoPlanUseCase();
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public FinalVideoExportResult export(FinalVideoExportRequest request) throws IOException {
        return export(request, ignored -> { }, () -> false);
    }

    public FinalVideoExportResult export(FinalVideoExportRequest request,
                                         Consumer<VideoRenderProgress> progress) throws IOException {
        return export(request, progress, () -> false);
    }

    public FinalVideoExportResult export(FinalVideoExportRequest request,
                                         Consumer<VideoRenderProgress> progress,
                                         BooleanSupplier cancellationRequested) throws IOException {
        Objects.requireNonNull(request, "request");
        return renderer.render(FinalVideoRenderRequest.withoutOverlays(request, buildPlan(request)),
                progress, cancellationRequested);
    }

    private SimpleVideoPlan buildPlan(FinalVideoExportRequest request) throws IOException {
        DocuPodcastProject project = request.project();
        if (project != null
                && project.metadata().mode()
                == com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode.NARRATIVE_VIDEO) {
            return buildNarrativeVideoPlan.build(
                    project, request.script(), request.jobs(), request.projectDirectory());
        }
        ProjectAssetCatalog assets = project == null ? ProjectAssetCatalog.empty() : project.assets();
        if (request.renderUnitPlan() != null && !request.renderUnitPlan().videoUnits().isEmpty()) {
            return buildSimpleVideoPlan.build(request.renderUnitPlan(), assets, request.jobs(),
                    request.settings().silenceAfterFrameSeconds());
        }
        if (request.script() == null || request.script().empty()) {
            throw new IllegalArgumentException("No hay lectura preparada para exportar video.");
        }
        return buildSimpleVideoPlan.build(request.script(), request.storyboard(), assets, request.jobs(),
                request.settings().silenceAfterFrameSeconds());
    }
}
