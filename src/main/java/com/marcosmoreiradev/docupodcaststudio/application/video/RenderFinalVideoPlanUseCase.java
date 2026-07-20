package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationArtifactStaging;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import com.marcosmoreiradev.docupodcaststudio.media.api.TimelineAudioTrack;
import com.marcosmoreiradev.docupodcaststudio.media.api.TimelineVisualKind;
import com.marcosmoreiradev.docupodcaststudio.media.api.TimelineVisualSource;
import com.marcosmoreiradev.docupodcaststudio.media.api.VideoEncodingPreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.VideoRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.VideoRenderResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.VideoTimelineItem;
import com.marcosmoreiradev.docupodcaststudio.media.api.VideoTimelinePlan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Application-level translation from the product video plan to the neutral media contract.
 * Process execution, codec selection and temporary files belong to the selected render adapter.
 */
public final class RenderFinalVideoPlanUseCase {
    private final MediaCapabilityService mediaCapabilities;

    public RenderFinalVideoPlanUseCase(MediaCapabilityService mediaCapabilities) {
        this.mediaCapabilities = Objects.requireNonNull(mediaCapabilities, "media capabilities");
    }

    public FinalVideoExportResult render(FinalVideoRenderRequest request) throws IOException {
        return render(request, ignored -> { }, () -> false);
    }

    public FinalVideoExportResult render(FinalVideoRenderRequest request,
                                         Consumer<VideoRenderProgress> progress) throws IOException {
        return render(request, progress, () -> false);
    }

    public FinalVideoExportResult render(FinalVideoRenderRequest request,
                                         Consumer<VideoRenderProgress> progress,
                                         BooleanSupplier cancellationRequested) throws IOException {
        Objects.requireNonNull(request, "request");
        Consumer<VideoRenderProgress> safeProgress = progress == null ? ignored -> { } : progress;
        BooleanSupplier cancel = cancellationRequested == null ? () -> false : cancellationRequested;
        SimpleVideoPlan productPlan = request.plan();
        safeProgress.accept(VideoRenderProgress.preparing(productPlan.frameCount()));
        if (!productPlan.exportableAsRenderedVideo()) {
            throw new IOException("No se puede renderizar el video final: faltan imágenes o audio. "
                    + "Frames sin imagen: " + productPlan.framesMissingImage()
                    + "; frames hablados sin audio: " + productPlan.framesMissingAudio() + ".");
        }

        VideoTimelinePlan timeline = toNeutralPlan(request);
        ExecutionContext context = new ExecutionContext(
                "final-video-render",
                cancellationToken(cancel),
                (stage, ratio, message) -> safeProgress.accept(toProductProgress(
                        stage, ratio, message, productPlan.frameCount())),
                ExecutionPolicy.defaults(),
                ResourceLease.NONE,
                GenerationArtifactStaging.NONE);
        try {
            VideoRenderResult result = mediaCapabilities.render(null,
                    new VideoRenderRequest(timeline, request.targetMp4(), Map.of()), context);
            safeProgress.accept(VideoRenderProgress.completed(productPlan.frameCount(), result.videoFile().toString()));
            long bytes = java.nio.file.Files.size(result.videoFile());
            List<String> diagnostics = result.diagnostics().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList();
            return new FinalVideoExportResult(result.videoFile(), result.videoFile().getParent(),
                    request.settings().resolution(), productPlan.frameCount(), result.durationSeconds(), bytes, diagnostics);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            safeProgress.accept(VideoRenderProgress.cancelled(0, productPlan.frameCount()));
            throw new IOException("La exportación de video fue cancelada.", ex);
        }
    }

    private static VideoTimelinePlan toNeutralPlan(FinalVideoRenderRequest request) {
        Path projectRoot = request.projectDirectory();
        List<VideoTimelineItem> items = request.plan().frames().stream()
                .map(frame -> toNeutralItem(projectRoot, frame))
                .toList();
        List<TimelineAudioTrack> overlays = request.audioOverlayPlan().inputs().stream()
                .map(RenderFinalVideoPlanUseCase::toNeutralOverlay)
                .toList();
        SimpleVideoExportSettings settings = request.settings();
        return new VideoTimelinePlan(items, overlays,
                settings.resolution().width(), settings.resolution().height(), settings.framesPerSecond(),
                encodingPreference(settings), Map.of("source", "simple-video-plan"));
    }

    private static VideoTimelineItem toNeutralItem(Path projectRoot, SimpleVideoFrame frame) {
        ArrayList<TimelineVisualSource> visuals = new ArrayList<>();
        for (SimpleVideoFrame.VisualPart part : frame.visualParts()) {
            visuals.add(new TimelineVisualSource(
                    part.videoClip() ? TimelineVisualKind.VIDEO_CLIP : TimelineVisualKind.STILL_IMAGE,
                    projectRoot.resolve(part.imageRelativePath()).normalize(),
                    part.sourceStartSeconds(), part.durationSeconds(), part.label(),
                    Map.of("assetId", part.imageAssetId())));
        }
        Path narration = frame.silentVisual() || frame.audioRelativePath().isBlank()
                ? null : projectRoot.resolve(frame.audioRelativePath()).normalize();
        return new VideoTimelineItem(frame.id(), visuals, narration, frame.frameDurationSeconds(), Map.of(
                "segmentId", frame.segmentId(),
                "imageAssetId", frame.imageAssetId(),
                "substitute", Boolean.toString(!frame.imageAssigned())));
    }

    private static TimelineAudioTrack toNeutralOverlay(VideoAudioOverlayPlan.Input input) {
        return new TimelineAudioTrack(input.overlayId(), input.audioFile(), input.sourceStartSeconds(),
                input.sourceEndSeconds(), input.timelineStartSeconds(), input.volume(),
                input.fadeDurationSeconds(), input.fadeDurationSeconds(), Map.of());
    }

    private static VideoEncodingPreference encodingPreference(SimpleVideoExportSettings settings) {
        if (settings.encoderPolicy().hardwareAccelerated()) return VideoEncodingPreference.HARDWARE_PREFERRED;
        return settings.encoderPolicy().name().contains("CPU")
                ? VideoEncodingPreference.CPU : VideoEncodingPreference.AUTO;
    }

    private static CancellationToken cancellationToken(BooleanSupplier cancellationRequested) {
        return cancellationRequested::getAsBoolean;
    }

    private static VideoRenderProgress toProductProgress(String stage, double ratio, String message, int total) {
        int completed = Math.max(0, Math.min(total, (int) Math.floor(ratio * total)));
        if ("COMPLETED".equalsIgnoreCase(stage)) return VideoRenderProgress.verifying(total, message);
        if ("MIXING".equalsIgnoreCase(stage)) return VideoRenderProgress.mixing(total, message);
        return VideoRenderProgress.rendering(completed, total, message);
    }
}
