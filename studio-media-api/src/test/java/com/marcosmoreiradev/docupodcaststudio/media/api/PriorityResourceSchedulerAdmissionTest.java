package com.marcosmoreiradev.docupodcaststudio.media.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class PriorityResourceSchedulerAdmissionTest {
    private static final long MIB = 1024L * 1024L;
    private static final ComputeDeviceId GPU = ComputeDeviceId.gpu("nvidia", 0);

    @Test
    void physicallyIneligibleXttsDoesNotBlockPdfRenderAndRunsAfterBatchBoundary()
            throws Exception {
        PriorityResourceScheduler scheduler = physicalScheduler();
        ResourceLease qwenBatch = scheduler.acquire(request("qwen-batch",
                ComputeJobPriority.INTERACTIVE_ANALYSIS, qwenDemand(),
                CancellationToken.NONE, OperationDeadline.none()));
        qwenBatch.confirmModelResident();
        qwenBatch.close();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch xttsAcquired = new CountDownLatch(1);
            Future<?> xtts = executor.submit(() -> {
                try (ResourceLease ignored = scheduler.acquire(request("xtts",
                        ComputeJobPriority.USER_AUDIO, xttsDemand(),
                        CancellationToken.NONE, OperationDeadline.none()))) {
                    xttsAcquired.countDown();
                }
                return null;
            });
            awaitQueued(scheduler, "xtts");
            ComputeQueueSnapshot.Entry blocked = queued(scheduler, "xtts");
            assertFalse(blocked.fits());
            assertEquals("VRAM_BUDGET", blocked.fitFailureReason());
            assertEquals(3_612L * MIB,
                    blocked.fitDetails().get("projectedBytes"));

            try (ResourceLease ignored = scheduler.acquire(request("pdf-render",
                    ComputeJobPriority.INTERACTIVE_ANALYSIS, cpuDemand(),
                    CancellationToken.NONE,
                    OperationDeadline.after(Duration.ofSeconds(2))))) {
                assertEquals(1L, queued(scheduler, "xtts").bypassCount());
                assertEquals(1L, xttsAcquired.getCount());
            }

            qwenBatch.confirmModelUnloaded();
            assertTrue(xttsAcquired.await(2, TimeUnit.SECONDS));
            xtts.get(2, TimeUnit.SECONDS);
        } finally {
            qwenBatch.close();
        }
        assertTrue(scheduler.snapshot().active().isEmpty());
        assertTrue(scheduler.snapshot().queued().isEmpty());
    }

    @Test
    void higherPriorityStillWinsWhenBothRequestsFit() throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                Map.of(ResourceId.CPU_HEAVY, 1));
        ResourceLease holder = scheduler.acquire(request("holder",
                ComputeJobPriority.BACKGROUND, cpuDemand(), CancellationToken.NONE,
                OperationDeadline.none()));
        List<String> order = java.util.Collections.synchronizedList(new ArrayList<>());
        CountDownLatch releaseFirst = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> lower = queuedTask(executor, scheduler, "analysis",
                    ComputeJobPriority.INTERACTIVE_ANALYSIS, order, releaseFirst);
            awaitQueued(scheduler, "analysis");
            Future<?> higher = queuedTask(executor, scheduler, "audio",
                    ComputeJobPriority.USER_AUDIO, order, releaseFirst);
            awaitQueued(scheduler, "audio");
            holder.close();
            awaitOrderSize(order, 1);
            assertEquals("audio", order.getFirst());
            releaseFirst.countDown();
            higher.get(2, TimeUnit.SECONDS);
            lower.get(2, TimeUnit.SECONDS);
        } finally {
            holder.close();
        }
    }

    @Test
    void bypassedHighPriorityRecoversPrecedenceAsSoonAsItFits() throws Exception {
        PriorityResourceScheduler scheduler = physicalScheduler();
        ResourceLease qwenBatch = scheduler.acquire(request("qwen-batch",
                ComputeJobPriority.INTERACTIVE_ANALYSIS, qwenDemand(),
                CancellationToken.NONE, OperationDeadline.none()));
        qwenBatch.confirmModelResident();
        qwenBatch.close();
        List<String> order = java.util.Collections.synchronizedList(new ArrayList<>());
        CountDownLatch releaseFirst = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> xtts = queuedTask(executor, scheduler, "xtts",
                    ComputeJobPriority.USER_AUDIO, xttsDemand(), order, releaseFirst);
            awaitQueued(scheduler, "xtts");
            for (int index = 1; index <= 2; index++) {
                try (ResourceLease ignored = scheduler.acquire(request("render-" + index,
                        ComputeJobPriority.INTERACTIVE_ANALYSIS, cpuDemand(),
                        CancellationToken.NONE,
                        OperationDeadline.after(Duration.ofSeconds(2))))) { }
            }
            ResourceLease cpuHolder = scheduler.acquire(request("cpu-holder",
                    ComputeJobPriority.PLAYBACK_CRITICAL,
                    new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 2),
                            0L, 0L, true, ComputeDeviceId.CPU_0, 0, null,
                            EncoderResourceDemand.none()),
                    CancellationToken.NONE, OperationDeadline.none()));
            Future<?> nextRender = queuedTask(executor, scheduler, "render-3",
                    ComputeJobPriority.INTERACTIVE_ANALYSIS, cpuDemand(), order,
                    releaseFirst);
            awaitQueued(scheduler, "render-3");
            long bypasses = queued(scheduler, "xtts").bypassCount();
            assertTrue(bypasses >= 2L);
            qwenBatch.confirmModelUnloaded();
            cpuHolder.close();
            awaitOrderSize(order, 1);
            assertEquals("xtts", order.getFirst());
            releaseFirst.countDown();
            xtts.get(2, TimeUnit.SECONDS);
            nextRender.get(2, TimeUnit.SECONDS);
        } finally {
            qwenBatch.close();
        }
    }

    @Test
    void admissionDeadlineExpiresAndLeavesNoZombieRequest() throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                Map.of(ResourceId.CPU_HEAVY, 1));
        try (ResourceLease ignored = scheduler.acquire(request("holder",
                ComputeJobPriority.BACKGROUND, cpuDemand(), CancellationToken.NONE,
                OperationDeadline.none()))) {
            ResourceAdmissionTimeoutException failure = assertThrows(
                    ResourceAdmissionTimeoutException.class,
                    () -> scheduler.acquire(request("timed", ComputeJobPriority.USER_AUDIO,
                            cpuDemand(), CancellationToken.NONE,
                            OperationDeadline.after(Duration.ofMillis(150)))));
            assertEquals("CPU_HEAVY_CAPACITY", failure.fitFailureReason());
            assertTrue(scheduler.snapshot().queued().isEmpty());
        }
        try (ResourceLease ignored = scheduler.acquire(request("after-timeout",
                ComputeJobPriority.BACKGROUND, cpuDemand(), CancellationToken.NONE,
                OperationDeadline.after(Duration.ofSeconds(1))))) { }
    }

    @Test
    void cancellationRemovesQueuedRequestAndWakesScheduler() throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                Map.of(ResourceId.CPU_HEAVY, 1));
        AtomicBoolean cancelled = new AtomicBoolean();
        ResourceLease holder = scheduler.acquire(request("holder",
                ComputeJobPriority.BACKGROUND, cpuDemand(), CancellationToken.NONE,
                OperationDeadline.none()));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> waiting = executor.submit(() -> scheduler.acquire(request(
                    "cancelled", ComputeJobPriority.USER_AUDIO, cpuDemand(),
                    cancelled::get, OperationDeadline.none())));
            awaitQueued(scheduler, "cancelled");
            cancelled.set(true);
            assertThrows(Exception.class, () -> waiting.get(1, TimeUnit.SECONDS));
            assertTrue(scheduler.snapshot().queued().isEmpty());
            holder.close();
            try (ResourceLease ignored = scheduler.acquire(request("next",
                    ComputeJobPriority.BACKGROUND, cpuDemand(), CancellationToken.NONE,
                    OperationDeadline.after(Duration.ofSeconds(1))))) { }
        } finally {
            holder.close();
        }
    }

    private static Future<?> queuedTask(java.util.concurrent.ExecutorService executor,
                                        PriorityResourceScheduler scheduler,
                                        String id, ComputeJobPriority priority,
                                        List<String> order,
                                        CountDownLatch release) {
        return queuedTask(executor, scheduler, id, priority, cpuDemand(), order,
                release);
    }

    private static Future<?> queuedTask(java.util.concurrent.ExecutorService executor,
                                        PriorityResourceScheduler scheduler,
                                        String id, ComputeJobPriority priority,
                                        ComputeResourceDemand demand,
                                        List<String> order,
                                        CountDownLatch release) {
        return executor.submit(() -> {
            try (ResourceLease ignored = scheduler.acquire(request(id, priority,
                    demand, CancellationToken.NONE,
                    OperationDeadline.after(Duration.ofSeconds(5))))) {
                order.add(id);
                release.await(2, TimeUnit.SECONDS);
            }
            return null;
        });
    }

    private static PriorityResourceScheduler physicalScheduler() {
        ComputeResourceBudget budget = new ComputeResourceBudget(Map.of(
                ResourceId.CPU_HEAVY, 2, ResourceId.QWEN_INFERENCE, 1),
                32L * 1024L * MIB, 8L * 1024L * MIB,
                Map.of(GPU, new ComputeDeviceBudget(GPU, 4L * 1024L * MIB,
                        3_072L * MIB, 1, Map.of())),
                3_072L * MIB, 1, 1);
        return new PriorityResourceScheduler(budget);
    }

    private static ComputeResourceDemand qwenDemand() {
        return new ComputeResourceDemand(Map.of(ResourceId.QWEN_INFERENCE, 1,
                ResourceId.CPU_HEAVY, 1), 0L, 0L, true, GPU, 0,
                new ModelResidencyDemand(new ModelResidencyKey("qwen", "4b-q8",
                        "ctx8k", GPU), 4_000L * MIB, 1_800L * MIB),
                EncoderResourceDemand.none());
    }

    private static ComputeResourceDemand xttsDemand() {
        return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                384L * MIB, 512L * MIB, true, GPU, 1,
                new ModelResidencyDemand(new ModelResidencyKey("xtts", "v2",
                        "gpu", GPU), 1_500L * MIB, 1_300L * MIB),
                EncoderResourceDemand.none());
    }

    private static ComputeResourceDemand cpuDemand() {
        return ComputeResourceDemand.of(ResourceId.CPU_HEAVY);
    }

    private static ComputeAdmissionRequest request(String id,
                                                   ComputeJobPriority priority,
                                                   ComputeResourceDemand demand,
                                                   CancellationToken cancellation,
                                                   OperationDeadline deadline) {
        return new ComputeAdmissionRequest(id, id, priority,
                ComputeWorkloadKind.OTHER, demand, cancellation, deadline);
    }

    private static void awaitQueued(PriorityResourceScheduler scheduler, String id)
            throws InterruptedException {
        long end = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < end) {
            if (scheduler.snapshot().queued().stream()
                    .anyMatch(entry -> entry.operationId().equals(id))) return;
            Thread.sleep(10L);
        }
        fail("request was not queued: " + id);
    }

    private static ComputeQueueSnapshot.Entry queued(
            PriorityResourceScheduler scheduler, String id) {
        return scheduler.snapshot().queued().stream()
                .filter(entry -> entry.operationId().equals(id)).findFirst()
                .orElseThrow();
    }

    private static void awaitOrderSize(List<String> order, int size)
            throws InterruptedException {
        long end = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (System.nanoTime() < end) {
            if (order.size() >= size) return;
            Thread.sleep(10L);
        }
        fail("no request was admitted");
    }
}
