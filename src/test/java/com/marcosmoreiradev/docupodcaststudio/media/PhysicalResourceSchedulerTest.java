package com.marcosmoreiradev.docupodcaststudio.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class PhysicalResourceSchedulerTest {
    private static final long MIB = 1024L * 1024L;
    private static final ComputeDeviceId GPU0 = ComputeDeviceId.gpu("NVIDIA", 0);
    private static final ComputeDeviceId GPU1 = ComputeDeviceId.gpu("INTEL", 1);

    @Test
    void sharedResidencyIsChargedOnceRetainedAndReleasedOnlyOnRealUnload()
            throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(budget());
        ModelResidencyKey key = new ModelResidencyKey(
                "qwen", "qwen3-vl:4b-q8", "ctx8k-kvq8", GPU0);
        ComputeResourceDemand demand = demand(GPU0, key,
                2_000L * MIB, 1_000L * MIB,
                200L * MIB, 100L * MIB, 1);

        try (ResourceLease first = scheduler.acquire(request("first", demand))) {
            first.confirmModelResident();
            assertEquals(2_200L * MIB,
                    scheduler.snapshot().reservedHostMemoryBytes());
        }
        assertEquals(2_000L * MIB,
                scheduler.snapshot().reservedHostMemoryBytes());
        assertEquals(1, scheduler.snapshot().modelResidencies().size());

        try (ResourceLease second = scheduler.acquire(request("second", demand))) {
            assertEquals(2_200L * MIB,
                    scheduler.snapshot().reservedHostMemoryBytes(),
                    "same model must pay only marginal request memory");
        }
        assertEquals(1, scheduler.snapshot().modelResidencies().size());

        try (ResourceLease unload = scheduler.acquire(request("unload", demand))) {
            unload.confirmModelUnloaded();
        }
        assertTrue(scheduler.snapshot().modelResidencies().isEmpty());
        assertEquals(0L, scheduler.snapshot().reservedHostMemoryBytes());
    }

    @Test
    void distinctModelsHaveIndependentResidenciesAndCancellationReleasesOnlyMarginal()
            throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(budget());
        ModelResidencyKey qwen = new ModelResidencyKey(
                "ollama", "qwen", "q8", GPU0);
        ModelResidencyKey xtts = new ModelResidencyKey(
                "xtts", "xtts-v2", "cuda", GPU0);
        ComputeResourceDemand qwenDemand = demand(GPU0, qwen,
                1_500L * MIB, 800L * MIB, 100L * MIB, 50L * MIB, 0);
        ComputeResourceDemand xttsDemand = demand(GPU0, xtts,
                1_000L * MIB, 700L * MIB, 100L * MIB, 50L * MIB, 0);
        try (ResourceLease lease = scheduler.acquire(request("qwen", qwenDemand))) {
            lease.confirmModelResident();
        }
        try (ResourceLease lease = scheduler.acquire(request("xtts", xttsDemand))) {
            lease.confirmModelResident();
        }
        assertEquals(2, scheduler.snapshot().modelResidencies().size());

        try (ResourceLease cancelled = scheduler.acquire(
                request("cancelled", qwenDemand))) {
            assertTrue(scheduler.cancel("cancelled"));
            assertTrue(cancelled.cancellationRequested());
        }
        assertEquals(2, scheduler.snapshot().modelResidencies().size(),
                "request cancellation must not evict confirmed weights");
    }

    @Test
    void gpuComputeVramAndEncoderAreScopedByPhysicalDevice() throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(budget());
        ComputeResourceDemand gpu0 = new ComputeResourceDemand(Map.of(),
                0L, 500L * MIB, true, GPU0, 1, null,
                new EncoderResourceDemand(VideoEncoderKind.NVIDIA_NVENC, GPU0, 1));
        ComputeResourceDemand gpu1 = new ComputeResourceDemand(Map.of(),
                0L, 500L * MIB, true, GPU1, 1, null,
                new EncoderResourceDemand(VideoEncoderKind.INTEL_QSV, GPU1, 1));

        try (ResourceLease first = scheduler.acquire(request("gpu0", gpu0));
             ResourceLease second = scheduler.acquire(request("gpu1", gpu1))) {
            var snapshot = scheduler.snapshot();
            assertEquals(2, snapshot.active().size());
            assertEquals(1, snapshot.deviceReservations().get(GPU0)
                    .encoderUnits().get(VideoEncoderKind.NVIDIA_NVENC));
            assertEquals(1, snapshot.deviceReservations().get(GPU1)
                    .encoderUnits().get(VideoEncoderKind.INTEL_QSV));
        }
    }

    @Test
    void impossibleHostAndVramDemandsAreRejectedBeforeQueueing() {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(budget());
        assertThrows(IllegalArgumentException.class, () -> scheduler.acquire(
                request("host-too-large", new ComputeResourceDemand(Map.of(),
                        9_000L * MIB, 0L, true, ComputeDeviceId.CPU_0,
                        0, null, EncoderResourceDemand.none()))));
        assertThrows(IllegalArgumentException.class, () -> scheduler.acquire(
                request("vram-too-large", new ComputeResourceDemand(Map.of(),
                        0L, 5_000L * MIB, true, GPU0,
                        1, null, EncoderResourceDemand.none()))));
        assertTrue(scheduler.snapshot().queued().isEmpty());
    }

    @Test
    void cumulativeBudgetsAndGpuComputeAreAdmittedAtomicallyPerDevice()
            throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(budget());
        ComputeResourceDemand ownerDemand = new ComputeResourceDemand(Map.of(),
                4_500L * MIB, 2_000L * MIB, true, GPU0,
                2, null, EncoderResourceDemand.none());
        ComputeResourceDemand blockedDemand = new ComputeResourceDemand(Map.of(),
                4_000L * MIB, 1_000L * MIB, true, GPU0,
                1, null, EncoderResourceDemand.none());
        ComputeResourceDemand otherDeviceDemand = new ComputeResourceDemand(Map.of(),
                500L * MIB, 500L * MIB, true, GPU1,
                1, null, EncoderResourceDemand.none());
        AtomicBoolean cancel = new AtomicBoolean();

        try (ResourceLease owner = scheduler.acquire(request("owner", ownerDemand));
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var waiting = executor.submit(() -> {
                try (ResourceLease ignored = scheduler.acquire(new ComputeAdmissionRequest(
                        "blocked", "blocked", ComputeJobPriority.BACKGROUND,
                        ComputeWorkloadKind.CONTENT_ANALYSIS, blockedDemand,
                        cancel::get))) {
                    return false;
                } catch (InterruptedException expected) {
                    return true;
                }
            });
            awaitQueued(scheduler, "blocked");

            try (ResourceLease other = scheduler.acquire(
                    request("other-device", otherDeviceDemand))) {
                assertEquals("other-device", other.admissionId(),
                        "GPU0 saturation must not reserve partial host/VRAM or block GPU1");
                assertEquals(2, scheduler.snapshot().active().size());
            }

            cancel.set(true);
            assertTrue(waiting.get(2, TimeUnit.SECONDS));
        }
        assertEquals(0L, scheduler.snapshot().reservedHostMemoryBytes());
        assertTrue(scheduler.snapshot().deviceReservations().isEmpty());
    }

    private static ComputeResourceBudget budget() {
        ComputeDeviceBudget gpu0 = new ComputeDeviceBudget(GPU0,
                4_096L * MIB, 3_500L * MIB, 2,
                Map.of(VideoEncoderKind.NVIDIA_NVENC, 1));
        ComputeDeviceBudget gpu1 = new ComputeDeviceBudget(GPU1,
                6_144L * MIB, 5_000L * MIB, 2,
                Map.of(VideoEncoderKind.INTEL_QSV, 1));
        return new ComputeResourceBudget(Map.of(ResourceId.CPU_HEAVY, 2,
                ResourceId.QWEN_INFERENCE, 1),
                16_000L * MIB, 8_000L * MIB,
                Map.of(GPU0, gpu0, GPU1, gpu1), 0L, 0, 0);
    }

    private static ComputeResourceDemand demand(
            ComputeDeviceId device, ModelResidencyKey key,
            long residentHost, long residentVram,
            long marginalHost, long marginalVram, int compute) {
        return new ComputeResourceDemand(Map.of(), marginalHost, marginalVram,
                true, device, compute,
                new ModelResidencyDemand(key, residentHost, residentVram),
                EncoderResourceDemand.none());
    }

    private static ComputeAdmissionRequest request(
            String id, ComputeResourceDemand demand) {
        return new ComputeAdmissionRequest(id, id,
                ComputeJobPriority.INTERACTIVE_ANALYSIS,
                ComputeWorkloadKind.CONTENT_ANALYSIS,
                demand, CancellationToken.NONE);
    }

    private static void awaitQueued(PriorityResourceScheduler scheduler, String id)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (scheduler.snapshot().queued().stream()
                    .anyMatch(entry -> entry.admissionId().equals(id))) return;
            Thread.sleep(10L);
        }
        fail("Timed out waiting for queued admission " + id);
    }
}
