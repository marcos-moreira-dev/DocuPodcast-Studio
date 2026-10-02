package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Submits an explicit scope through the shared single-worker scheduler. */
public final class PreparePdfScopeUseCase {
    private final ResolvePdfPreparationScopeUseCase resolveScope;
    private final PdfPagePreparationScheduler scheduler;

    public PreparePdfScopeUseCase(ResolvePdfPreparationScopeUseCase resolveScope,
                                  PdfPagePreparationScheduler scheduler) {
        this.resolveScope = Objects.requireNonNull(resolveScope, "resolveScope");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public CompletableFuture<PdfPreparationScopeResult> execute(
            PdfPreparationScopeRequest request,
            Path cacheDirectory,
            PdfPreparationPriority priority,
            PdfProjectSessionToken session,
            Consumer<PdfPreparationScopeProgress> progressConsumer) {
        return execute(request, cacheDirectory, priority, session, progressConsumer, null);
    }

    /**
     * Reports a page only after {@link PreparePdfPageUseCase} has validated and atomically
     * published its canonical {@code PreparedPdfPage}. Consumers may therefore start derived
     * work such as narration/TTS without observing provisional inference output.
     */
    public CompletableFuture<PdfPreparationScopeResult> execute(
            PdfPreparationScopeRequest request,
            Path cacheDirectory,
            PdfPreparationPriority priority,
            PdfProjectSessionToken session,
            Consumer<PdfPreparationScopeProgress> progressConsumer,
            Consumer<PreparePdfPageResult> acceptedPageConsumer) {
        PdfPreparationScopeResolution resolution = resolveScope.resolve(request);
        if (resolution.requiresManualRange() || resolution.pages().isEmpty()) {
            return CompletableFuture.completedFuture(new PdfPreparationScopeResult(
                    resolution, List.of(), List.of(), List.of(), false));
        }
        String sha = request.workspace().sourceSha256();
        com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest preferences;
        try {
            preferences = resolveScope.preferences(request.workspace());
        } catch (java.io.IOException failure) {
            return CompletableFuture.failedFuture(failure);
        }
        List<CompletableFuture<PreparePdfPageResult>> futures = new ArrayList<>();
        long started = System.nanoTime();
        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger accepted = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger rejected = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger failed = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger cancelled = new java.util.concurrent.atomic.AtomicInteger();
        for (int page : resolution.pages()) {
            com.marcosmoreiradev.docupodcaststudio.media.api.ProgressSink pageProgress =
                    (stage, fraction, message) -> {
                        if (progressConsumer != null) {
                            progressConsumer.accept(new PdfPreparationScopeProgress(
                                    completed.get(), resolution.pages().size(), page,
                                    -1L, message, accepted.get(), rejected.get(),
                                    failed.get(), cancelled.get()));
                        }
                    };
            CompletableFuture<PreparePdfPageResult> future = scheduler.submit(
                    new PdfPreparationTaskKey(sha, page), priority, session,
                    new PreparePdfPageRequest(request.workspace(), cacheDirectory,
                            page, false, null, priority, pageProgress,
                            request.origin(), request.retryAllowed(), request.scopeId(),
                            preferences.readingStrategy(), preferences.nativeTextProvider()));
            future.thenAccept(result -> {
                if (result.succeeded() && acceptedPageConsumer != null) {
                    acceptedPageConsumer.accept(result);
                }
                if (result.succeeded()) accepted.incrementAndGet();
                else if (result.rejected()) rejected.incrementAndGet();
                else if (result.cancelled()) cancelled.incrementAndGet();
                else failed.incrementAndGet();
                int done = completed.incrementAndGet();
                long estimate = done < 3 ? -1 : estimateRemainingSeconds(started, done, resolution.pages().size());
                if (progressConsumer != null) {
                    progressConsumer.accept(new PdfPreparationScopeProgress(
                            done, resolution.pages().size(), page, estimate,
                            result.terminalMessage(), accepted.get(), rejected.get(),
                            failed.get(), cancelled.get()));
                }
            });
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> aggregate(resolution, futures));
    }

    private static PdfPreparationScopeResult aggregate(PdfPreparationScopeResolution resolution,
                                                       List<CompletableFuture<PreparePdfPageResult>> futures) {
        ArrayList<Integer> prepared = new ArrayList<>();
        ArrayList<Integer> failed = new ArrayList<>();
        ArrayList<String> uncertain = new ArrayList<>();
        ArrayList<PreparePdfPageResult> outcomes = new ArrayList<>();
        boolean cancelled = false;
        for (CompletableFuture<PreparePdfPageResult> future : futures) {
            PreparePdfPageResult result = future.join();
            outcomes.add(result);
            cancelled |= result.cancelled();
            if (result.succeeded()) {
                prepared.add(result.pageNumber());
                result.preparedPage().regions().stream()
                        .filter(region -> PdfRegionReviewStatus.requiresReview(
                                result.preparedPage(), region))
                        .map(region -> region.id()).forEach(uncertain::add);
            } else if (!result.cancelled()) {
                failed.add(result.pageNumber());
            }
        }
        return new PdfPreparationScopeResult(
                resolution, prepared, failed, uncertain, cancelled, outcomes);
    }

    private static long estimateRemainingSeconds(long startedNanos, int completed, int total) {
        if (completed <= 0 || total <= completed) return 0L;
        double seconds = (System.nanoTime() - startedNanos) / 1_000_000_000.0;
        return Math.max(0L, Math.round((seconds / completed) * (total - completed)));
    }
}
