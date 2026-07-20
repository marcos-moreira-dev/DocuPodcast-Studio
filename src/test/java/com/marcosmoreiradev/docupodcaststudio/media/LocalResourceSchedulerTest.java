package com.marcosmoreiradev.docupodcaststudio.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class LocalResourceSchedulerTest {
    @Test
    void oppositeDeclarationOrderCannotDeadlock() throws Exception {
        LocalResourceScheduler scheduler = new LocalResourceScheduler(Map.of(ResourceId.GPU, 1, ResourceId.MODEL_MEMORY, 1));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> acquire(scheduler,
                    ResourceRequirement.of(ResourceId.GPU, ResourceId.MODEL_MEMORY), start));
            var second = executor.submit(() -> acquire(scheduler,
                    ResourceRequirement.of(ResourceId.MODEL_MEMORY, ResourceId.GPU), start));
            start.countDown();
            assertTrue(first.get(2, TimeUnit.SECONDS));
            assertTrue(second.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void cancellationWhileWaitingReleasesAlreadyAcquiredResources() throws Exception {
        LocalResourceScheduler scheduler = LocalResourceScheduler.safeDefaults();
        AtomicBoolean cancel = new AtomicBoolean();
        try (ResourceLease occupied = scheduler.acquire(ResourceRequirement.of(ResourceId.GPU), CancellationToken.NONE);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var waiting = executor.submit(() -> {
                try {
                    scheduler.acquire(ResourceRequirement.of(ResourceId.MODEL_MEMORY, ResourceId.GPU), cancel::get);
                    return false;
                } catch (InterruptedException expected) {
                    return true;
                }
            });
            Thread.sleep(120);
            cancel.set(true);
            assertTrue(waiting.get(2, TimeUnit.SECONDS));
        }
        try (ResourceLease released = scheduler.acquire(
                ResourceRequirement.of(ResourceId.MODEL_MEMORY, ResourceId.GPU), CancellationToken.NONE)) {
            assertNotNull(released);
        }
    }

    private static boolean acquire(LocalResourceScheduler scheduler, ResourceRequirement requirement,
                                   CountDownLatch start) throws Exception {
        assertTrue(start.await(1, TimeUnit.SECONDS));
        try (ResourceLease ignored = scheduler.acquire(requirement, CancellationToken.NONE)) {
            Thread.sleep(30);
            return true;
        }
    }
}
