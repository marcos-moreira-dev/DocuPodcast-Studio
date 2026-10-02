package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class MediaCapabilityPhysicalDemandTest {
    @TempDir Path temp;

    @Test
    void pageReadingCarriesResidencyMarginalMemoryComputeAndSerialLane()
            throws Exception {
        AtomicReference<ComputeAdmissionRequest> captured = new AtomicReference<>();
        ResourceScheduler scheduler = request -> {
            captured.set(request);
            return ResourceLease.NONE;
        };
        ContentAnalysisEngine engine = new FakeContentEngine();
        ContentAnalysisEngineRegistry contents =
                new ContentAnalysisEngineRegistry().register(engine);
        MediaEnginePlatform platform = new MediaEnginePlatform(
                null, null, null, null, null, null, contents, null);
        MediaCapabilityService service = new MediaCapabilityService(
                platform, scheduler, () -> ComputePreference.preferGpu(true));
        Path page = temp.resolve("page.png");
        Files.write(page, new byte[]{1});

        service.analyzeContent(engine.descriptor().id(),
                new ContentAnalysisRequest(
                        ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                        List.of(new AnalysisVisualInput(page,
                                "complete-page", "image/png")),
                        "read", "", "es", "{}",
                        Map.of("contextWindowTokens", "8192",
                                "batchSize", "512")),
                ExecutionContext.defaults("page"));

        ComputeResourceDemand demand = captured.get().demand();
        assertEquals(1, demand.units().get(ResourceId.QWEN_INFERENCE));
        assertEquals(1, demand.units().get(ResourceId.CPU_HEAVY));
        assertEquals(1, demand.gpuComputeUnits());
        assertTrue(demand.estimatedHostMemoryBytes() > 0L);
        assertTrue(demand.estimatedVramBytes() > 0L);
        assertNotNull(demand.modelResidency());
        assertTrue(demand.modelResidency().key().runtimeProfile().contains("ctx8192"));
        assertEquals(ComputeDeviceId.AUTO_GPU_0, demand.device());
    }

    @Test
    void eightBProfileCarriesItsOwnLargerPhysicalDemand() throws Exception {
        AtomicReference<ComputeAdmissionRequest> captured = new AtomicReference<>();
        ResourceScheduler scheduler = request -> {
            captured.set(request);
            return ResourceLease.NONE;
        };
        ContentAnalysisEngine engine = new FakeContentEngine();
        ContentAnalysisEngineRegistry contents =
                new ContentAnalysisEngineRegistry().register(engine);
        MediaCapabilityService service = new MediaCapabilityService(
                new MediaEnginePlatform(null, null, null, null, null, null,
                        contents, null), scheduler,
                () -> ComputePreference.preferGpu(true));
        Path page = temp.resolve("page-8b.png");
        Files.write(page, new byte[]{1});

        service.analyzeContent(engine.descriptor().id(),
                new ContentAnalysisRequest(ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                        List.of(new AnalysisVisualInput(page, "complete-page", "image/png")),
                        "read", "", "es", "{}", Map.of(
                        "model", PdfVlmRuntimeProfile.MODEL_8B_Q8,
                        "contextWindowTokens", "8192", "batchSize", "512",
                        "kvCacheType", "q8_0", "flashAttention", "true",
                        "modelHostMib", "7475", "modelVramMib", "2300",
                        "marginalHostMib", "640", "marginalVramMib", "512")),
                ExecutionContext.defaults("page-8b"));

        ComputeResourceDemand demand = captured.get().demand();
        assertEquals(7_475L * 1024L * 1024L,
                demand.modelResidency().hostMemoryBytes());
        assertEquals(2_300L * 1024L * 1024L,
                demand.modelResidency().vramBytes());
        assertTrue(demand.modelResidency().key().runtimeProfile()
                .contains(PdfVlmRuntimeProfile.MODEL_8B_Q8));
    }

    @Test
    void qsvRenderClaimsIntelEncoderWithoutBecomingNvenc() throws Exception {
        AtomicReference<ComputeAdmissionRequest> captured = new AtomicReference<>();
        ResourceScheduler scheduler = request -> {
            captured.set(request);
            return ResourceLease.NONE;
        };
        EngineRegistry<VideoRenderEngine> renders =
                new EngineRegistry<>(CapabilityId.VIDEO_RENDERING);
        renders.register(new FakeRenderEngine());
        MediaCapabilityService service = new MediaCapabilityService(
                new MediaEnginePlatform(null, null, null, renders, null),
                scheduler);
        VideoTimelinePlan plan = new VideoTimelinePlan(List.of(
                new VideoTimelineItem("one", List.of(
                        new TimelineVisualSource(TimelineVisualKind.STILL_IMAGE,
                                temp.resolve("image.png"), 0, 1, "", Map.of())),
                        null, 1, Map.of())), List.of(), 1280, 720, 30,
                VideoEncodingPreference.INTEL_QSV, Map.of());

        service.render(null, new VideoRenderRequest(plan,
                        temp.resolve("video.mp4"),
                        Map.of("computeDeviceId", "GPU:INTEL:2")),
                ExecutionContext.defaults("video"));

        EncoderResourceDemand encoder = captured.get().demand().encoder();
        assertEquals(VideoEncoderKind.INTEL_QSV, encoder.kind());
        assertEquals(ComputeDeviceId.gpu("INTEL", 2), encoder.device());
        assertEquals("h264_qsv", plan.encodingPreference()
                .exactKind().ffmpegCodec());
    }

    private static final class FakeContentEngine implements ContentAnalysisEngine {
        private static final EngineId ID = new EngineId("qwen3-vl-local");
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(ID, CapabilityId.VISUAL_CONTENT_DESCRIPTION,
                    "fake", "1", "test", Set.of(), true);
        }
        @Override public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.PAGE_SEMANTIC_READING);
        }
        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(ID, List.of());
        }
        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(ID, "ready");
        }
        @Override public ContentAnalysisResult analyze(
                ContentAnalysisRequest request, ExecutionContext context) {
            return new ContentAnalysisResult("", "{}", 0, List.of(), Map.of());
        }
    }

    private static final class FakeRenderEngine implements VideoRenderEngine {
        private static final EngineId ID = new EngineId("fake-render");
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(ID, CapabilityId.VIDEO_RENDERING,
                    "fake", "1", "test", Set.of(), true);
        }
        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(ID, List.of());
        }
        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(ID, "ready");
        }
        @Override public VideoRenderResult render(
                VideoRenderRequest request, ExecutionContext context) {
            return new VideoRenderResult(request.outputFile(), 1, Map.of());
        }
    }
}
