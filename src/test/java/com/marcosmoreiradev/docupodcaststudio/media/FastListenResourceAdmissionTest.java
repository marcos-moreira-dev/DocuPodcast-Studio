package com.marcosmoreiradev.docupodcaststudio.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FastListenResourceAdmissionTest {
    @Test
    void admitsPiperBesideOneQwenButNeverASecondQwenInference()
            throws Exception {
        PriorityResourceScheduler scheduler = new PriorityResourceScheduler(
                ComputeResourceBudget.incrementalReaderDefaults());
        ComputeResourceDemand qwen = new ComputeResourceDemand(
                Map.of(ResourceId.QWEN_INFERENCE, 1, ResourceId.CPU_HEAVY, 1),
                256L * 1024L * 1024L, 256L * 1024L * 1024L, true,
                ComputeDeviceId.AUTO_GPU_0, 1, null,
                EncoderResourceDemand.none());
        ComputeResourceDemand piper = new ComputeResourceDemand(
                Map.of(ResourceId.CPU_HEAVY, 1),
                192L * 1024L * 1024L, 0L, true,
                ComputeDeviceId.CPU_0, 0, null,
                EncoderResourceDemand.none());

        try (ResourceLease firstQwen = scheduler.acquire(request("qwen-p2", qwen));
             ResourceLease pageOneTts = scheduler.acquire(request("piper-p1", piper));
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            assertEquals(2, scheduler.snapshot().active().size());
            var secondQwen = executor.submit(() -> {
                try (ResourceLease ignored = scheduler.acquire(
                        request("qwen-p3", qwen))) {
                    return true;
                }
            });
            awaitQueued(scheduler, "qwen-p3");
            assertTrue(!secondQwen.isDone(),
                    "QWEN_INFERENCE=1 debe conservar Qwen estrictamente serial");
            assertTrue(scheduler.cancel("qwen-p3"));
            assertThrows(ExecutionException.class,
                    () -> secondQwen.get(2, TimeUnit.SECONDS));
        }
    }

    private static ComputeAdmissionRequest request(
            String id, ComputeResourceDemand demand) {
        return new ComputeAdmissionRequest(id, id,
                ComputeJobPriority.INTERACTIVE_ANALYSIS,
                ComputeWorkloadKind.CONTENT_ANALYSIS,
                demand, CancellationToken.NONE);
    }

    private static void awaitQueued(PriorityResourceScheduler scheduler,
                                    String id) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (scheduler.snapshot().queued().stream()
                    .anyMatch(entry -> entry.admissionId().equals(id))) return;
            Thread.sleep(10L);
        }
        throw new AssertionError("No se encoló " + id);
    }
}
