package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeDeviceId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.ModelResidencyKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ManagedOllamaProcessLifecycleTest {
    @TempDir Path root;

    @Test
    void twoRequestsShareWeightsAndOnlyLastClosePerformsUnload() throws Exception {
        AtomicInteger unloadPosts = new AtomicInteger();
        try (ManagedOllamaProcess process = process(unloadPosts, new AtomicInteger())) {
            ModelResidencyKey model = model(ComputeDeviceId.AUTO_GPU_0);
            var a = process.openModelRequest(model, context("A", gpu()));
            var b = process.openModelRequest(model, context("B", gpu()));
            a.confirmModelLoaded();
            b.confirmModelLoaded();
            a.unloadWhenIdle();
            b.unloadWhenIdle();

            a.close();
            assertEquals(0, unloadPosts.get());
            assertEquals(1, process.lifecycleSnapshot().activeRequests());
            assertTrue(process.lifecycleSnapshot().residentModels().contains(model));

            b.close();
            assertEquals(1, unloadPosts.get());
            assertTrue(process.lifecycleSnapshot().residentModels().isEmpty());
        }
    }

    @Test
    void computePreferenceRestartWaitsForExistingRequestSafePoint() throws Exception {
        AtomicInteger launches = new AtomicInteger();
        try (ManagedOllamaProcess process = process(new AtomicInteger(), launches);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = process.openModelRequest(
                    model(ComputeDeviceId.AUTO_GPU_0), context("A", gpu()));
            var bFuture = executor.submit(() -> process.openModelRequest(
                    model(ComputeDeviceId.CPU_0), context("B", cpu())));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (process.lifecycleSnapshot().pendingPreference() == null
                    && System.nanoTime() < deadline) Thread.onSpinWait();
            assertEquals(gpu(), a.preference());
            assertEquals(1, launches.get());

            a.close();
            try (var b = bFuture.get(3, TimeUnit.SECONDS)) {
                assertEquals(cpu(), b.preference());
                assertEquals(2, launches.get());
            }
        }
    }

    @Test
    void runtimeDeathInvalidatesEveryActiveRequestAndResidency() throws Exception {
        AtomicReference<Process> child = new AtomicReference<>();
        AtomicInteger unloads = new AtomicInteger();
        try (ManagedOllamaProcess process = process(unloads, new AtomicInteger(), child)) {
            ModelResidencyKey model = model(ComputeDeviceId.AUTO_GPU_0);
            try (var a = process.openModelRequest(model, context("A", gpu()));
                 var b = process.openModelRequest(model, context("B", gpu()))) {
                a.confirmModelLoaded();
                b.confirmModelLoaded();
                child.get().destroyForcibly().waitFor(2, TimeUnit.SECONDS);

                EngineExecutionException aFailure = assertThrows(
                        EngineExecutionException.class, a::assertRuntimeAlive);
                EngineExecutionException bFailure = assertThrows(
                        EngineExecutionException.class, b::assertRuntimeAlive);
                assertEquals(EngineDiagnosticCode.CHILD_EXIT, aFailure.code());
                assertEquals(EngineDiagnosticCode.CHILD_EXIT, bFailure.code());
                assertTrue(process.lifecycleSnapshot().residentModels().isEmpty());
            }
        }
    }

    private ManagedOllamaProcess process(AtomicInteger unloadPosts,
                                         AtomicInteger launches) throws Exception {
        return process(unloadPosts, launches, new AtomicReference<>());
    }

    private ManagedOllamaProcess process(AtomicInteger unloadPosts,
                                         AtomicInteger launches,
                                         AtomicReference<Process> child) throws Exception {
        Path executable = root.resolve("ollama.exe");
        Files.writeString(executable, "fixture");
        return new ManagedOllamaProcess(executable, root.resolve("models"),
                root.resolve("logs"), () -> 19443,
                ignored -> {
                    launches.incrementAndGet();
                    Process started = new ProcessBuilder("cmd", "/c",
                            "ping -t 127.0.0.1 >nul").start();
                    child.set(started);
                    return started;
                }, new ManagedOllamaProcess.EndpointClient() {
                    @Override public boolean ready(java.net.URI endpoint) { return true; }
                    @Override public ManagedOllamaProcess.EndpointResponse post(
                            java.net.URI endpoint, String body, Duration timeout) {
                        if (body.contains("keep_alive")) unloadPosts.incrementAndGet();
                        return new ManagedOllamaProcess.EndpointResponse(200, "{}");
                    }
                    @Override public ManagedOllamaProcess.EndpointResponse get(
                            java.net.URI endpoint, Duration timeout) {
                        return new ManagedOllamaProcess.EndpointResponse(200, "{}");
                    }
                }, System::nanoTime);
    }

    private static ExecutionContext context(String id, ComputePreference preference) {
        ExecutionContext base = ExecutionContext.defaults(id);
        return new ExecutionContext(id, base.cancellation(), base.progress(),
                base.policy(), base.resourceLease(), base.staging(), preference);
    }

    private static ComputePreference gpu() {
        return ComputePreference.preferGpu(true);
    }

    private static ComputePreference cpu() {
        return ComputePreference.cpuOnly(true);
    }

    private static ModelResidencyKey model(ComputeDeviceId device) {
        return new ModelResidencyKey("ollama", "qwen", "q8-kvq8-flash", device);
    }
}
