package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;
import java.util.Objects;

/** Mode-independent input for rendering an already prepared final-video plan. */
public record FinalVideoRenderRequest(
        SimpleVideoPlan plan,
        Path projectDirectory,
        Path targetMp4,
        SimpleVideoExportSettings settings,
        Path applicationRoot,
        Path configuredFfmpeg,
        VideoAudioOverlayPlan audioOverlayPlan
) {
    public FinalVideoRenderRequest {
        plan = Objects.requireNonNull(plan, "plan");
        projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory").toAbsolutePath().normalize();
        targetMp4 = Objects.requireNonNull(targetMp4, "targetMp4").toAbsolutePath().normalize();
        settings = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        applicationRoot = applicationRoot == null
                ? Path.of(".").toAbsolutePath().normalize()
                : applicationRoot.toAbsolutePath().normalize();
        configuredFfmpeg = configuredFfmpeg == null ? null : configuredFfmpeg.toAbsolutePath().normalize();
        audioOverlayPlan = audioOverlayPlan == null ? VideoAudioOverlayPlan.emptyPlan() : audioOverlayPlan;
    }

    public static FinalVideoRenderRequest withoutOverlays(FinalVideoExportRequest request, SimpleVideoPlan plan) {
        Objects.requireNonNull(request, "request");
        return new FinalVideoRenderRequest(plan, request.projectDirectory(), request.targetMp4(), request.settings(),
                request.applicationRoot(), request.configuredFfmpeg(), VideoAudioOverlayPlan.emptyPlan());
    }

    public FinalVideoRenderRequest withAudioOverlayPlan(VideoAudioOverlayPlan overlays) {
        return new FinalVideoRenderRequest(plan, projectDirectory, targetMp4, settings, applicationRoot,
                configuredFfmpeg, overlays);
    }
}
