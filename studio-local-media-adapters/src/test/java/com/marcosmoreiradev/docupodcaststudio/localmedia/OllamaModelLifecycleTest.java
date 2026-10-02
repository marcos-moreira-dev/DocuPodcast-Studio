package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeDeviceId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.ModelResidencyKey;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

final class OllamaModelLifecycleTest {
    private static final ComputePreference GPU = ComputePreference.preferGpu(true);
    private static final ComputePreference CPU = ComputePreference.cpuOnly(true);
    private static final ModelResidencyKey QWEN = model("qwen", ComputeDeviceId.AUTO_GPU_0);

    @Test
    void oneRequestFinishingOrCancellingCannotUnloadUnderAnotherRequest() {
        OllamaModelLifecycle lifecycle = ready();
        var a = acquired(lifecycle, "A", QWEN, GPU);
        var b = acquired(lifecycle, "B", QWEN, GPU);
        lifecycle.confirmModelLoaded(QWEN);

        assertEquals(OllamaModelLifecycle.ActionType.NONE,
                lifecycle.finishRequest(a, true).type());
        assertTrue(lifecycle.requestRuntimeValid(b));
        assertTrue(lifecycle.snapshot().residentModels().contains(QWEN));

        var unload = lifecycle.finishRequest(b, true);
        assertEquals(OllamaModelLifecycle.ActionType.UNLOAD_MODEL, unload.type());
        lifecycle.completeUnload(QWEN, true);
        assertTrue(lifecycle.snapshot().residentModels().isEmpty());
    }

    @Test
    void batchOwnershipDefersUnloadPastEndBatchUntilActiveRequestFinishes() {
        OllamaModelLifecycle lifecycle = ready();
        lifecycle.retainResidency("document-1", QWEN);
        var page = acquired(lifecycle, "P1", QWEN, GPU);
        lifecycle.confirmModelLoaded(QWEN);

        assertEquals(OllamaModelLifecycle.ActionType.NONE,
                lifecycle.releaseResidency("document-1", true).type());
        assertEquals(1, lifecycle.snapshot().activeRequests());
        assertTrue(lifecycle.snapshot().pendingUnloads().contains(QWEN));

        assertEquals(OllamaModelLifecycle.ActionType.UNLOAD_MODEL,
                lifecycle.finishRequest(page, false).type());
    }

    @Test
    void ownerKeepsWeightsResidentAfterLastRequestAndDifferentModelsDoNotShare() {
        OllamaModelLifecycle lifecycle = ready();
        ModelResidencyKey other = model("other", ComputeDeviceId.AUTO_GPU_0);
        lifecycle.retainResidency("batch", QWEN);
        var qwen = acquired(lifecycle, "Q", QWEN, GPU);
        lifecycle.confirmModelLoaded(QWEN);
        lifecycle.confirmModelLoaded(other);

        assertEquals(OllamaModelLifecycle.ActionType.NONE,
                lifecycle.finishRequest(qwen, true).type());
        assertEquals(2, lifecycle.snapshot().residentModels().size());
        assertEquals(OllamaModelLifecycle.ActionType.UNLOAD_MODEL,
                lifecycle.requestUnload(other).type());
        assertTrue(lifecycle.snapshot().residentModels().contains(QWEN));
    }

    @Test
    void preferenceChangeWaitsAndEachRequestKeepsItsImmutableSnapshot() {
        OllamaModelLifecycle lifecycle = ready();
        var a = acquired(lifecycle, "A", QWEN, GPU);
        var waiting = lifecycle.tryBeginRequest("B", model("qwen", ComputeDeviceId.CPU_0), CPU);
        assertEquals(OllamaModelLifecycle.BeginStatus.WAIT, waiting.status());
        assertEquals(GPU, a.preference());

        lifecycle.finishRequest(a, false);
        var restart = lifecycle.tryBeginRequest("B", model("qwen", ComputeDeviceId.CPU_0), CPU);
        assertEquals(OllamaModelLifecycle.BeginStatus.RESTART_REQUIRED, restart.status());
        lifecycle.runtimeStarted(CPU);
        var b = acquired(lifecycle, "B", model("qwen", ComputeDeviceId.CPU_0), CPU);
        assertEquals(CPU, b.preference());
        assertEquals(GPU, a.preference(), "existing request snapshot must not mutate");
    }

    @Test
    void runtimeDeathInvalidatesBothRequestsAndAllRealResidency() {
        OllamaModelLifecycle lifecycle = ready();
        var a = acquired(lifecycle, "A", QWEN, GPU);
        var b = acquired(lifecycle, "B", QWEN, GPU);
        lifecycle.confirmModelLoaded(QWEN);

        assertEquals(1, lifecycle.runtimeDied().size());
        assertFalse(lifecycle.requestRuntimeValid(a));
        assertFalse(lifecycle.requestRuntimeValid(b));
        assertTrue(lifecycle.snapshot().residentModels().isEmpty());
    }

    @Test
    void simultaneousFinishesProduceExactlyOneDeferredUnload() throws Exception {
        OllamaModelLifecycle lifecycle = ready();
        var a = acquired(lifecycle, "A", QWEN, GPU);
        var b = acquired(lifecycle, "B", QWEN, GPU);
        lifecycle.confirmModelLoaded(QWEN);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                start.await();
                return lifecycle.finishRequest(a, true);
            });
            var second = executor.submit(() -> {
                start.await();
                return lifecycle.finishRequest(b, true);
            });
            start.countDown();
            var x = first.get(2, TimeUnit.SECONDS);
            var y = second.get(2, TimeUnit.SECONDS);
            long unloads = java.util.stream.Stream.of(x, y)
                    .filter(action -> action.type()
                            == OllamaModelLifecycle.ActionType.UNLOAD_MODEL)
                    .count();
            assertEquals(1L, unloads);
        }
    }

    @Test
    void batchCloseAndRequestStartAreSafeInEitherOrdering() {
        OllamaModelLifecycle requestFirst = ready();
        requestFirst.retainResidency("batch", QWEN);
        requestFirst.confirmModelLoaded(QWEN);
        var active = acquired(requestFirst, "page", QWEN, GPU);
        assertEquals(OllamaModelLifecycle.ActionType.NONE,
                requestFirst.releaseResidency("batch", true).type());
        assertTrue(requestFirst.requestRuntimeValid(active));

        OllamaModelLifecycle closeFirst = ready();
        closeFirst.retainResidency("batch", QWEN);
        closeFirst.confirmModelLoaded(QWEN);
        assertEquals(OllamaModelLifecycle.ActionType.UNLOAD_MODEL,
                closeFirst.releaseResidency("batch", true).type());
        assertEquals(OllamaModelLifecycle.BeginStatus.WAIT,
                closeFirst.tryBeginRequest("page", QWEN, GPU).status());
    }

    @Test
    void shutdownRejectsNewRequestsAndInvalidatesExistingOnRuntimeStop() {
        OllamaModelLifecycle lifecycle = ready();
        var active = acquired(lifecycle, "A", QWEN, GPU);
        lifecycle.beginShutdown();
        assertEquals(OllamaModelLifecycle.BeginStatus.SHUTTING_DOWN,
                lifecycle.tryBeginRequest("B", QWEN, GPU).status());
        lifecycle.runtimeDied();
        assertFalse(lifecycle.requestRuntimeValid(active));
    }

    @Test
    void runtimeDeathRacingWithRequestFinishLeavesNoGhostRequest() throws Exception {
        OllamaModelLifecycle lifecycle = ready();
        var active = acquired(lifecycle, "A", QWEN, GPU);
        lifecycle.confirmModelLoaded(QWEN);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var finish = executor.submit(() -> {
                start.await();
                return lifecycle.finishRequest(active, true);
            });
            var death = executor.submit(() -> {
                start.await();
                return lifecycle.runtimeDied();
            });
            start.countDown();
            finish.get(2, TimeUnit.SECONDS);
            death.get(2, TimeUnit.SECONDS);
        }
        assertEquals(0, lifecycle.snapshot().activeRequests());
        assertTrue(lifecycle.snapshot().residentModels().isEmpty());
    }

    private static OllamaModelLifecycle ready() {
        OllamaModelLifecycle lifecycle = new OllamaModelLifecycle();
        lifecycle.runtimeStarted(GPU);
        return lifecycle;
    }

    private static OllamaModelLifecycle.RequestSnapshot acquired(
            OllamaModelLifecycle lifecycle, String id,
            ModelResidencyKey model, ComputePreference preference) {
        var result = lifecycle.tryBeginRequest(id, model, preference);
        assertEquals(OllamaModelLifecycle.BeginStatus.ACQUIRED, result.status());
        return result.request();
    }

    private static ModelResidencyKey model(String id, ComputeDeviceId device) {
        return new ModelResidencyKey("ollama", id, "q8-kvq8-flash", device);
    }
}
