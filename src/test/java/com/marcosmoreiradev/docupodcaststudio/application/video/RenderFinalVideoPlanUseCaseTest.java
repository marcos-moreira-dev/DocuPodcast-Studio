package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDevicePolicy;
import com.marcosmoreiradev.docupodcaststudio.application.compute.VideoEncoderPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class RenderFinalVideoPlanUseCaseTest {
    @TempDir Path tempDir;
    private Path projectDirectory;
    private Path overlayAudio;
    private CapturingRenderEngine engine;
    private RenderFinalVideoPlanUseCase renderer;

    @BeforeEach
    void setUp() throws Exception {
        projectDirectory = tempDir.resolve("project");
        Files.createDirectories(projectDirectory.resolve("media/images"));
        Files.createDirectories(projectDirectory.resolve("media/audio"));
        Files.createDirectories(projectDirectory.resolve("generated/narrative/clips"));
        Files.write(projectDirectory.resolve("media/images/frame.png"), new byte[]{1});
        Files.write(projectDirectory.resolve("media/audio/narration.wav"), new byte[]{1});
        Files.write(projectDirectory.resolve("generated/narrative/clips/take.mp4"), new byte[]{1});
        overlayAudio = projectDirectory.resolve("media/audio/ambience.wav");
        Files.write(overlayAudio, new byte[]{1});
        engine = new CapturingRenderEngine();
        EngineRegistry<VideoRenderEngine> renders = new EngineRegistry<>(CapabilityId.VIDEO_RENDERING);
        renders.register(engine);
        MediaEnginePlatform platform = new MediaEnginePlatform(null, null, null, renders, null);
        renderer = new RenderFinalVideoPlanUseCase(
                new MediaCapabilityService(platform, LocalResourceScheduler.safeDefaults()));
    }

    @Test
    void translatesProductPlanToNeutralTimeline() throws Exception {
        Path target = tempDir.resolve("plain.mp4");
        ArrayList<VideoRenderProgress> progress = new ArrayList<>();

        FinalVideoExportResult result = renderer.render(request(target, VideoAudioOverlayPlan.emptyPlan()),
                progress::add, () -> false);

        assertEquals(target.toAbsolutePath().normalize(), result.targetFile());
        assertTrue(Files.size(target) > 0L);
        VideoTimelinePlan plan = engine.lastRequest.effectivePlan();
        assertEquals(1280, plan.width());
        assertEquals(720, plan.height());
        assertEquals(24, plan.framesPerSecond());
        assertEquals(VideoEncodingPreference.CPU, plan.encodingPreference());
        assertEquals(projectDirectory.resolve("media/images/frame.png"), plan.items().getFirst().visuals().getFirst().file());
        assertEquals(projectDirectory.resolve("media/audio/narration.wav"), plan.items().getFirst().narrationAudio());
        assertTrue(progress.stream().anyMatch(item -> item.stage() == VideoRenderStage.COMPLETED));
    }

    @Test
    void translatesAudioOverlaysWithoutCodecDetails() throws Exception {
        VideoAudioOverlayPlan overlays = new VideoAudioOverlayPlan(List.of(
                new VideoAudioOverlayPlan.Input("AMBIENCE", overlayAudio, 0.1, 0.9, 0.4, 0.25, 0.1)));

        renderer.render(request(tempDir.resolve("overlay.mp4"), overlays));

        TimelineAudioTrack track = engine.lastRequest.effectivePlan().overlays().getFirst();
        assertEquals("AMBIENCE", track.id());
        assertEquals(overlayAudio.toAbsolutePath().normalize(), track.file());
        assertEquals(0.4, track.timelineStartSeconds());
        assertEquals(0.25, track.volume());
        assertEquals(0.1, track.fadeInSeconds());
    }

    @Test
    void preservesClipOffsetsAndDurations() throws Exception {
        SimpleVideoFrame.VisualPart clip = SimpleVideoFrame.VisualPart.videoClip(
                "VIDEO-001", "generated/narrative/clips/take.mp4", 1.25, 0.75, "clip-1");
        SimpleVideoFrame frame = new SimpleVideoFrame(
                "FRAME-001", "SEG-001", "Toma", "Texto", "IMG-001", "media/images/frame.png",
                "media/audio/narration.wav", 1.25, 0.0, true, true, false, List.of(), List.of(clip));
        SimpleVideoPlan plan = new SimpleVideoPlan("Narrativo", List.of(frame), 0.0, Instant.now());
        renderer.render(new FinalVideoRenderRequest(plan, projectDirectory, tempDir.resolve("clip.mp4"),
                settings(), tempDir, null, VideoAudioOverlayPlan.emptyPlan()));

        TimelineVisualSource source = engine.lastRequest.effectivePlan().items().getFirst().visuals().getFirst();
        assertEquals(TimelineVisualKind.VIDEO_CLIP, source.kind());
        assertEquals(0.75, source.sourceStartSeconds());
        assertEquals(1.25, source.durationSeconds());
    }

    @Test
    void cancellationReleasesSharedResourcesAndReportsCancelled() {
        ArrayList<VideoRenderProgress> progress = new ArrayList<>();
        IOException failure = assertThrows(IOException.class, () -> renderer.render(
                request(tempDir.resolve("cancelled.mp4"), VideoAudioOverlayPlan.emptyPlan()),
                progress::add, () -> true));
        assertTrue(failure.getMessage().contains("cancelada"));
        assertTrue(progress.stream().anyMatch(item -> item.stage() == VideoRenderStage.CANCELLED));
    }

    private FinalVideoRenderRequest request(Path target, VideoAudioOverlayPlan overlays) {
        SimpleVideoFrame frame = new SimpleVideoFrame("FRAME-001", "SEG-001", "Escena", "Texto",
                "IMG-001", "media/images/frame.png", "media/audio/narration.wav", 1.0, 0.0, true, true);
        return new FinalVideoRenderRequest(
                new SimpleVideoPlan("Prueba", List.of(frame), 0.0, Instant.now()),
                projectDirectory, target, settings(), tempDir, null, overlays);
    }

    private static SimpleVideoExportSettings settings() {
        return new SimpleVideoExportSettings(SimpleVideoResolutionPreset.HD_720, 24, 0.0, true, true,
                ComputeDevicePolicy.CPU_ONLY, VideoEncoderPolicy.CPU_X264);
    }

    private static final class CapturingRenderEngine implements VideoRenderEngine {
        private static final EngineId ID = new EngineId("fake-render");
        private VideoRenderRequest lastRequest;

        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(ID, CapabilityId.VIDEO_RENDERING, "Fake render", "1", "test", Set.of(), true);
        }

        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(ID, List.of());
        }

        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(ID, "ready");
        }

        @Override public VideoRenderResult render(VideoRenderRequest request, ExecutionContext context)
                throws IOException, InterruptedException {
            context.cancellation().throwIfCancellationRequested();
            lastRequest = request;
            Files.createDirectories(request.outputFile().toAbsolutePath().normalize().getParent());
            Files.write(request.outputFile(), new byte[]{1, 2, 3});
            context.progress().report("RENDERING", 0.5, "rendering");
            context.progress().report("COMPLETED", 1.0, "done");
            return new VideoRenderResult(request.outputFile(), request.effectivePlan().durationSeconds(), Map.of());
        }
    }
}
