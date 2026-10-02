package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeAdmissionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisBatchLifecycle;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisEngine;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisEngineRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfigurationSchema;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import com.marcosmoreiradev.docupodcaststudio.media.api.OperationDeadline;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceScheduler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaCapabilityServiceContentBatchTest {
    @Test
    void keepsOneLeaseAndOneModelLifecycleForSeveralIndependentRequests()
            throws Exception {
        BatchEngine engine = new BatchEngine();
        ContentAnalysisEngineRegistry registry =
                new ContentAnalysisEngineRegistry().register(engine);
        MediaEnginePlatform platform = new MediaEnginePlatform(
                null, null, null, null, null, null, registry, null);
        AtomicInteger acquired = new AtomicInteger();
        AtomicInteger released = new AtomicInteger();
        AtomicReference<ComputeAdmissionRequest> admission = new AtomicReference<>();
        ResourceScheduler scheduler = request -> {
            admission.set(request);
            acquired.incrementAndGet();
            return released::incrementAndGet;
        };
        MediaCapabilityService service =
                new MediaCapabilityService(platform, scheduler);
        ContentAnalysisRequest first = request("primera");
        ContentAnalysisRequest second = request("segunda");

        OperationDeadline batchDeadline = OperationDeadline.after(Duration.ofSeconds(30));
        service.withContentAnalysisBatch(
                ExecutionContext.defaults("document-listening-batch")
                        .withDeadline(batchDeadline), () -> {
                    service.analyzeContent(engine.descriptor().id(), first,
                            ExecutionContext.defaults("first"));
                    service.analyzeContent(engine.descriptor().id(), second,
                            ExecutionContext.defaults("second"));
                    return null;
                });

        assertEquals(1, acquired.get());
        assertEquals(1, released.get());
        assertEquals(1, engine.beginCount.get());
        assertEquals(1, engine.endCount.get());
        assertEquals(2, engine.analysisCount.get());
        assertNotSame(ResourceLease.NONE, engine.lastLease);
        assertTrue(engine.lastDeadline == batchDeadline,
                "requests inside the batch must retain its single absolute deadline");
        assertTrue(admission.get().demand().units().containsKey(
                ResourceId.QWEN_INFERENCE));
        assertTrue(admission.get().demand().units().containsKey(ResourceId.CPU_HEAVY),
                "GPU+host offload must expose its CPU cost conservatively");
        assertFalse(admission.get().demand().units().containsKey(ResourceId.MODEL_MEMORY));
        assertTrue(admission.get().demand().gpuComputeUnits() > 0);
        assertTrue(admission.get().demand().estimatedHostMemoryBytes() > 0L);
        assertTrue(admission.get().demand().estimatedVramBytes() > 0L);
        assertTrue(admission.get().demand().modelResidency() != null);
    }

    @Test
    void releasesBatchLeaseAfterTimeoutAndCancellation() throws Exception {
        BatchEngine engine = new BatchEngine();
        ContentAnalysisEngineRegistry registry =
                new ContentAnalysisEngineRegistry().register(engine);
        MediaEnginePlatform platform = new MediaEnginePlatform(
                null, null, null, null, null, null, registry, null);
        AtomicInteger released = new AtomicInteger();
        ResourceScheduler scheduler = request -> released::incrementAndGet;
        MediaCapabilityService service = new MediaCapabilityService(platform, scheduler);

        engine.technicalFailure = new EngineExecutionException(
                EngineDiagnosticCode.RUNTIME_UNRESPONSIVE,
                "runtime de prueba sin respuesta", Map.of());
        assertThrows(EngineExecutionException.class, () ->
                service.withContentAnalysisBatch(
                        ExecutionContext.defaults("timeout-batch"), () -> {
                            service.analyzeContent(engine.descriptor().id(), request("timeout"),
                                    ExecutionContext.defaults("timeout-request"));
                            return null;
                        }));
        assertEquals(1, released.get());
        assertEquals(1, engine.endCount.get());

        engine.technicalFailure = null;
        engine.interrupt = true;
        assertThrows(InterruptedException.class, () ->
                service.withContentAnalysisBatch(
                        ExecutionContext.defaults("cancel-batch"), () -> {
                            service.analyzeContent(engine.descriptor().id(), request("cancel"),
                                    ExecutionContext.defaults("cancel-request"));
                            return null;
                        }));
        assertEquals(2, released.get());
        assertEquals(2, engine.endCount.get());
    }

    @Test
    void textTranslationGetsADeadlineShorterThanTheFormerTwelveMinutes()
            throws Exception {
        TranslationDeadlineEngine engine = new TranslationDeadlineEngine();
        MediaEnginePlatform platform = new MediaEnginePlatform(
                null, null, null, null, null, null,
                new ContentAnalysisEngineRegistry().register(engine), null);
        MediaCapabilityService service = new MediaCapabilityService(
                platform, request -> ResourceLease.NONE);

        service.translateNarration("La conclusión es válida.", "es", "en",
                ExecutionContext.defaults("translation-deadline"));

        assertTrue(engine.deadline.bounded());
        assertTrue(engine.deadline.remainingMillis() <= Duration.ofMinutes(5).toMillis());
        assertTrue(engine.deadline.remainingMillis() > Duration.ofMinutes(4).toMillis());
    }

    private static ContentAnalysisRequest request(String instruction) {
        return new ContentAnalysisRequest(
                ContentAnalysisOperation.CONTEXT_CORRECTION, List.of(),
                instruction, "", "es", "", Map.of());
    }

    private static final class BatchEngine
            implements ContentAnalysisEngine, ContentAnalysisBatchLifecycle {
        private final AtomicInteger beginCount = new AtomicInteger();
        private final AtomicInteger endCount = new AtomicInteger();
        private final AtomicInteger analysisCount = new AtomicInteger();
        private ResourceLease lastLease = ResourceLease.NONE;
        private OperationDeadline lastDeadline = OperationDeadline.none();
        private EngineExecutionException technicalFailure;
        private boolean interrupt;

        @Override
        public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId("batch-test"),
                    CapabilityId.CONTENT_CONTEXT_CORRECTION,
                    "Batch test", "1", "test", Set.of(), true);
        }

        @Override
        public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.CONTEXT_CORRECTION);
        }

        @Override
        public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(
                    descriptor().id(), List.of());
        }

        @Override
        public EngineReadiness inspectReadiness(
                EngineConfiguration configuration) {
            return EngineReadiness.ready(
                    descriptor().id(), "Ready for test");
        }

        @Override
        public ContentAnalysisResult analyze(
                ContentAnalysisRequest request, ExecutionContext context)
                throws IOException, InterruptedException {
            analysisCount.incrementAndGet();
            lastLease = context.resourceLease();
            lastDeadline = context.deadline();
            if (technicalFailure != null) throw technicalFailure;
            if (interrupt) throw new InterruptedException("cancelled in test");
            return new ContentAnalysisResult(
                    request.instruction(), "", 1.0, List.of(), Map.of());
        }

        @Override
        public void beginContentAnalysisBatch(ExecutionContext context) {
            beginCount.incrementAndGet();
        }

        @Override
        public void endContentAnalysisBatch(ExecutionContext context) {
            endCount.incrementAndGet();
        }
    }

    private static final class TranslationDeadlineEngine implements ContentAnalysisEngine {
        private OperationDeadline deadline = OperationDeadline.none();
        private final EngineId id = new EngineId("translation-deadline-test");
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(id, CapabilityId.NARRATION_TRANSLATION,
                    "Translation deadline", "1", "test", Set.of(), true);
        }
        @Override public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.NARRATION_TRANSLATION);
        }
        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(id, List.of());
        }
        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(id, "ready");
        }
        @Override public ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                                       ExecutionContext context) {
            deadline = context.deadline();
            return new ContentAnalysisResult("The conclusion is valid.", "", 1.0,
                    List.of(), Map.of());
        }
    }
}
