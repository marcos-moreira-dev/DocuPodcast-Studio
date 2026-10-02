package com.marcosmoreiradev.docupodcaststudio.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

final class PriorityResourceSchedulerTest {
    @Test
    void blockedCompoundRequestDoesNotReserveOneResource() throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(Map.of(
                ResourceId.GPU, 1, ResourceId.MODEL_MEMORY, 1));
        AtomicBoolean cancel = new AtomicBoolean();
        try (ResourceLease gpu = scheduler.acquire(request("gpu-owner",
                ComputeJobPriority.BACKGROUND,
                ComputeResourceDemand.of(ResourceId.GPU),
                CancellationToken.NONE));
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var compound = executor.submit(() -> {
                try (ResourceLease ignored = scheduler.acquire(request(
                        "compound", ComputeJobPriority.BACKGROUND,
                        ComputeResourceDemand.of(
                                ResourceId.GPU, ResourceId.MODEL_MEMORY),
                        cancel::get))) {
                    return false;
                } catch (InterruptedException expected) {
                    return true;
                }
            });
            awaitQueued(scheduler, "compound");
            try (ResourceLease memory = scheduler.acquire(request(
                    "memory-only", ComputeJobPriority.PLAYBACK_CRITICAL,
                    ComputeResourceDemand.of(ResourceId.MODEL_MEMORY),
                    CancellationToken.NONE))) {
                assertEquals("memory-only", memory.admissionId());
            }
            cancel.set(true);
            assertTrue(compound.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void higherPriorityConflictingWorkRunsFirstAndRequestsSafeYield() throws Exception {
        PriorityResourceScheduler scheduler = PriorityResourceScheduler.safeDefaults();
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch releaseOwner = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var owner = executor.submit(() -> {
                try (ResourceLease lease = scheduler.acquire(request(
                        "lookahead-owner", ComputeJobPriority.AUDIO_LOOKAHEAD,
                        ComputeResourceDemand.of(ResourceId.MODEL_MEMORY),
                        CancellationToken.NONE))) {
                    assertTrue(releaseOwner.await(2, TimeUnit.SECONDS));
                    assertTrue(lease.yieldRequested());
                }
                return true;
            });
            awaitActive(scheduler, "lookahead-owner");
            var background = executor.submit(() -> runAndRecord(
                    scheduler, "background", ComputeJobPriority.BACKGROUND, order));
            var critical = executor.submit(() -> runAndRecord(
                    scheduler, "critical", ComputeJobPriority.PLAYBACK_CRITICAL, order));
            awaitQueued(scheduler, "critical");
            releaseOwner.countDown();
            assertTrue(owner.get(2, TimeUnit.SECONDS));
            assertTrue(critical.get(2, TimeUnit.SECONDS));
            assertTrue(background.get(2, TimeUnit.SECONDS));
            assertEquals(List.of("critical", "background"), order);
        }
    }

    @Test
    void backgroundAgesOnlyUpToInteractivePriority() {
        AtomicLong clock = new AtomicLong();
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                Map.of(ResourceId.GPU, 1), Duration.ofSeconds(10), clock::get);
        AtomicBoolean cancel = new AtomicBoolean();
        try (ResourceLease ignored = assertDoesNotThrow(() -> scheduler.acquire(
                request("owner", ComputeJobPriority.PLAYBACK_CRITICAL,
                        ComputeResourceDemand.of(ResourceId.GPU),
                        CancellationToken.NONE)));
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var waiting = executor.submit(() -> {
                try {
                    scheduler.acquire(request("aged", ComputeJobPriority.BACKGROUND,
                            ComputeResourceDemand.of(ResourceId.GPU), cancel::get));
                } catch (InterruptedException expected) {
                    return;
                }
            });
            assertDoesNotThrow(() -> awaitQueued(scheduler, "aged"));
            clock.set(Duration.ofMinutes(10).toNanos());
            ComputeQueueSnapshot.Entry entry = scheduler.snapshot().queued().getFirst();
            assertEquals(ComputeJobPriority.INTERACTIVE_ANALYSIS,
                    entry.effectivePriority());
            cancel.set(true);
            assertDoesNotThrow(() -> waiting.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void queuedCancellationAndActiveSafeBoundaryCancellationAreObservable()
            throws Exception {
        PriorityResourceScheduler scheduler =
                PriorityResourceScheduler.safeDefaults();
        try (ResourceLease owner = scheduler.acquire(request(
                "voice-active", ComputeJobPriority.USER_AUDIO,
                ComputeResourceDemand.of(ResourceId.MODEL_MEMORY),
                CancellationToken.NONE));
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var queued = executor.submit(() -> {
                try {
                    scheduler.acquire(request("qwen-queued",
                            ComputeJobPriority.INTERACTIVE_ANALYSIS,
                            ComputeResourceDemand.of(ResourceId.MODEL_MEMORY),
                            CancellationToken.NONE));
                    return false;
                } catch (InterruptedException expected) {
                    return true;
                }
            });
            awaitQueued(scheduler, "qwen-queued");
            assertTrue(scheduler.cancel("qwen-queued"));
            assertTrue(queued.get(2, TimeUnit.SECONDS));
            assertTrue(scheduler.cancel("voice-active"));
            assertTrue(owner.cancellationRequested());
            assertTrue(owner.yieldRequested());
        }
    }

    private static boolean runAndRecord(
            PriorityResourceScheduler scheduler,
            String id,
            ComputeJobPriority priority,
            List<String> order) throws InterruptedException {
        try (ResourceLease ignored = scheduler.acquire(request(
                id, priority, ComputeResourceDemand.of(ResourceId.MODEL_MEMORY),
                CancellationToken.NONE))) {
            order.add(id);
            Thread.sleep(20L);
            return true;
        }
    }

    private static ComputeAdmissionRequest request(
            String id,
            ComputeJobPriority priority,
            ComputeResourceDemand demand,
            CancellationToken cancellation) {
        return new ComputeAdmissionRequest(id, id, priority,
                ComputeWorkloadKind.OTHER, demand, cancellation);
    }

    private static void awaitQueued(
            PriorityResourceScheduler scheduler, String id) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (scheduler.snapshot().queued().stream()
                    .anyMatch(entry -> entry.admissionId().equals(id))) return;
            Thread.sleep(10L);
        }
        fail("Timed out waiting for queued admission " + id);
    }

    private static void awaitActive(
            PriorityResourceScheduler scheduler, String id) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (scheduler.snapshot().active().stream()
                    .anyMatch(entry -> entry.admissionId().equals(id))) return;
            Thread.sleep(10L);
        }
        fail("Timed out waiting for active admission " + id);
    }
}
