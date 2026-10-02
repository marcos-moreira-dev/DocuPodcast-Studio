package com.marcosmoreiradev.docupodcaststudio.application.document;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfPagePreparationSchedulerTest {
    @Test
    void passiveNavigationCannotCreateWork() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            calls.incrementAndGet();
            return success(request);
        })) {
            assertTrue(!scheduler.reprioritizePending(key(2),
                    PdfPreparationPriority.URGENT));
            Thread.sleep(50);
            assertEquals(0, calls.get());
            assertEquals(0, scheduler.snapshot().requested());
        }
    }

    @Test
    void passiveNavigationPromotesExistingItemWithoutDuplicatingIt()
            throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<Integer> order = java.util.Collections.synchronizedList(new ArrayList<>());
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            order.add(request.pageNumber());
            if (request.pageNumber() == 1) {
                firstStarted.countDown();
                await(release);
            }
            return success(request);
        })) {
            PreparedPdfWorkspaceRef workspace = workspace();
            PdfProjectSessionToken session = new PdfProjectSessionToken();
            var first = scheduler.submit(key(1), PdfPreparationPriority.URGENT,
                    session, request(workspace, 1));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            var page2 = scheduler.submit(key(2), PdfPreparationPriority.BACKGROUND,
                    session, request(workspace, 2));
            var page3 = scheduler.submit(key(3), PdfPreparationPriority.HIGH,
                    session, request(workspace, 3));
            assertTrue(scheduler.reprioritizePending(key(2),
                    PdfPreparationPriority.URGENT));
            assertTrue(scheduler.reprioritizePending(key(2),
                    PdfPreparationPriority.URGENT));
            assertEquals(3, scheduler.snapshot().requested());
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            page2.get(2, TimeUnit.SECONDS);
            page3.get(2, TimeUnit.SECONDS);
            assertEquals(List.of(1, 2, 3), List.copyOf(order));
        }
    }

    @Test
    void deduplicatesPromotesAndRunsOnlyOneWorker() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        List<Integer> order = java.util.Collections.synchronizedList(new ArrayList<>());
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            int now = active.incrementAndGet();
            maxActive.accumulateAndGet(now, Math::max);
            order.add(request.pageNumber());
            if (request.pageNumber() == 1) {
                firstStarted.countDown();
                await(releaseFirst);
            }
            active.decrementAndGet();
            return success(request);
        })) {
            PreparedPdfWorkspaceRef workspace = workspace();
            PdfProjectSessionToken session = new PdfProjectSessionToken();
            var page1 = scheduler.submit(key(1), PdfPreparationPriority.URGENT, session, request(workspace, 1));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            var page2a = scheduler.submit(key(2), PdfPreparationPriority.BACKGROUND, session, request(workspace, 2));
            var page2b = scheduler.submit(key(2), PdfPreparationPriority.HIGH, session, request(workspace, 2));
            var page3 = scheduler.submit(key(3), PdfPreparationPriority.URGENT, session, request(workspace, 3));
            releaseFirst.countDown();
            page1.get(2, TimeUnit.SECONDS);
            page2a.get(2, TimeUnit.SECONDS);
            page3.get(2, TimeUnit.SECONDS);
            assertTrue(page2a == page2b);
            assertEquals(List.of(1, 3, 2), order);
            assertEquals(1, maxActive.get());
        }
    }

    @Test
    void closingSessionCancelsQueuedAndSuppressesPublication() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            firstStarted.countDown();
            await(release);
            request.cancellationToken().throwIfCancelled();
            return success(request);
        })) {
            PreparedPdfWorkspaceRef workspace = workspace();
            PdfProjectSessionToken session = new PdfProjectSessionToken();
            var running = scheduler.submit(key(1), PdfPreparationPriority.URGENT, session, request(workspace, 1));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            var queued = scheduler.submit(key(2), PdfPreparationPriority.HIGH, session, request(workspace, 2));
            scheduler.closeSession(session);
            release.countDown();
            assertTrue(queued.get(2, TimeUnit.SECONDS).cancelled());
            assertTrue(running.handle((result, error) -> error != null || result.cancelled())
                    .get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void globalControlCancelsOnlyPagesThatHaveNotStarted() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (PdfPagePreparationScheduler scheduler =
                     new PdfPagePreparationScheduler(request -> {
                         if (request.pageNumber() == 1) {
                             firstStarted.countDown();
                             await(release);
                         }
                         return success(request);
                     })) {
            PreparedPdfWorkspaceRef workspace = workspace();
            PdfProjectSessionToken firstSession =
                    new PdfProjectSessionToken();
            PdfProjectSessionToken secondSession =
                    new PdfProjectSessionToken();
            var running = scheduler.submit(
                    key(1), PdfPreparationPriority.URGENT,
                    firstSession, request(workspace, 1));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            var queuedSameSession = scheduler.submit(
                    key(2), PdfPreparationPriority.HIGH,
                    firstSession, request(workspace, 2));
            var queuedOtherSession = scheduler.submit(
                    key(3), PdfPreparationPriority.HIGH,
                    secondSession, request(workspace, 3));

            scheduler.cancelAllPending();
            release.countDown();

            assertTrue(running.get(2, TimeUnit.SECONDS).succeeded());
            assertTrue(queuedSameSession.get(
                    2, TimeUnit.SECONDS).cancelled());
            assertTrue(queuedOtherSession.get(
                    2, TimeUnit.SECONDS).cancelled());
        }
    }

    @Test
    void queuedPagesShareOneResidentBatchWithoutQwenConcurrency() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger batchStarts = new AtomicInteger();
        AtomicInteger batchEnds = new AtomicInteger();
        CountDownLatch batchEnded = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        try (PdfPagePreparationScheduler scheduler =
                     new PdfPagePreparationScheduler((request, priority) -> {
                         maxActive.accumulateAndGet(active.incrementAndGet(), Math::max);
                         if (request.pageNumber() == 1) {
                             firstStarted.countDown();
                             await(releaseFirst);
                         }
                         active.decrementAndGet();
                         return success(request);
                     }, work -> {
                         batchStarts.incrementAndGet();
                         try {
                             work.run();
                         } finally {
                             batchEnds.incrementAndGet();
                             batchEnded.countDown();
                         }
                     })) {
            PreparedPdfWorkspaceRef workspace = workspace();
            PdfProjectSessionToken session = new PdfProjectSessionToken();
            var first = scheduler.submit(key(1), PdfPreparationPriority.URGENT,
                    session, request(workspace, 1));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            var second = scheduler.submit(key(2), PdfPreparationPriority.HIGH,
                    session, request(workspace, 2));
            var third = scheduler.submit(key(3), PdfPreparationPriority.NORMAL,
                    session, request(workspace, 3));
            releaseFirst.countDown();

            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
            third.get(2, TimeUnit.SECONDS);
            assertTrue(batchEnded.await(2, TimeUnit.SECONDS));
            assertEquals(1, batchStarts.get());
            assertEquals(1, batchEnds.get());
            assertEquals(1, maxActive.get());
        }
    }

    @Test
    void longDocumentNavigationOneToOneHundredFiftyToTwentyPromotesWithoutDuplicates()
            throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<Integer> order = java.util.Collections.synchronizedList(new ArrayList<>());
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        try (PdfPagePreparationScheduler scheduler = new PdfPagePreparationScheduler(request -> {
            maxActive.accumulateAndGet(active.incrementAndGet(), Math::max);
            order.add(request.pageNumber());
            if (request.pageNumber() == 1) {
                firstStarted.countDown();
                await(release);
            }
            active.decrementAndGet();
            return success(request);
        })) {
            PreparedPdfWorkspaceRef workspace = workspace();
            PdfProjectSessionToken session = new PdfProjectSessionToken();
            var first = scheduler.submit(key(1), PdfPreparationPriority.URGENT,
                    session, request(workspace, 1));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            var page20Background = scheduler.submit(key(20), PdfPreparationPriority.BACKGROUND,
                    session, request(workspace, 20));
            var page150 = scheduler.submit(key(150), PdfPreparationPriority.URGENT,
                    session, request(workspace, 150));
            var page20Urgent = scheduler.submit(key(20), PdfPreparationPriority.URGENT,
                    session, request(workspace, 20));
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            page150.get(2, TimeUnit.SECONDS);
            page20Urgent.get(2, TimeUnit.SECONDS);
            assertSame(page20Background, page20Urgent);
            assertEquals(List.of(1, 20, 150), order);
            assertEquals(1, maxActive.get());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) throw new AssertionError("timeout");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private static PreparePdfPageResult success(PreparePdfPageRequest request) {
        var page = new com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage(
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage
                        .CURRENT_SCHEMA_VERSION,
                request.pageNumber(), 612, 792,
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus.READY,
                1, List.of(), List.of(), "");
        return new PreparePdfPageResult(request.pageNumber(), page, true, false, List.of());
    }

    private static PreparePdfPageRequest request(PreparedPdfWorkspaceRef workspace, int page) {
        return new PreparePdfPageRequest(workspace, null, page, false, null);
    }

    private static PdfPreparationTaskKey key(int page) {
        return new PdfPreparationTaskKey("a".repeat(64), page);
    }

    private static PreparedPdfWorkspaceRef workspace() {
        return new PreparedPdfWorkspaceRef(java.nio.file.Path.of("workspace"),
                java.nio.file.Path.of("workspace/source/sample.pdf"), "a".repeat(64));
    }
}
