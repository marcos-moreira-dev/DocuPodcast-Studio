package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Single-worker scheduler for local PDF preparation.
 *
 * <p>Pending requests are deduplicated by source hash and page. A later,
 * stronger priority promotes the existing task; callers share its future.</p>
 */
public final class PdfPagePreparationScheduler implements AutoCloseable {
    @FunctionalInterface
    public interface Worker {
        PreparePdfPageResult prepare(PreparePdfPageRequest request);
    }

    @FunctionalInterface
    public interface PrioritizedWorker {
        PreparePdfPageResult prepare(PreparePdfPageRequest request,
                                     PdfPreparationPriority priority);
    }

    @FunctionalInterface
    public interface BatchRunner {
        void run(BatchWork work) throws Exception;
    }

    @FunctionalInterface
    public interface BatchWork {
        void run();
    }

    private final Object monitor = new Object();
    private final PrioritizedWorker worker;
    private final BatchRunner batchRunner;
    private final PriorityQueue<Entry> queue = new PriorityQueue<>(Comparator
            .comparingInt((Entry entry) -> entry.priority.rank())
            .thenComparingLong(entry -> entry.sequence));
    private final Map<PdfPreparationTaskKey, Entry> pending = new HashMap<>();
    private final List<Consumer<PdfPreparationProgress>> listeners = new CopyOnWriteArrayList<>();
    private final Thread thread;
    private long sequence;
    private boolean paused;
    private volatile boolean closed;
    private Entry running;
    private int requested;
    private int completed;

    public PdfPagePreparationScheduler(Worker worker) {
        this((request, ignored) -> Objects.requireNonNull(worker, "worker")
                .prepare(request));
    }

    public PdfPagePreparationScheduler(PrioritizedWorker worker) {
        this(worker, BatchWork::run);
    }

    public PdfPagePreparationScheduler(PrioritizedWorker worker,
                                       BatchRunner batchRunner) {
        this.worker = Objects.requireNonNull(worker, "worker");
        this.batchRunner = Objects.requireNonNullElse(
                batchRunner, BatchWork::run);
        thread = new Thread(this::runLoop, "docupodcast-pdf-preparation");
        thread.setDaemon(true);
        thread.start();
    }

    public CompletableFuture<PreparePdfPageResult> submit(PdfPreparationTaskKey key,
                                                          PdfPreparationPriority priority,
                                                          PdfProjectSessionToken session,
                                                          PreparePdfPageRequest request) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(request, "request");
        PdfPreparationPriority safePriority = priority == null ? PdfPreparationPriority.NORMAL : priority;
        synchronized (monitor) {
            if (closed || !session.active()) {
                return CompletableFuture.completedFuture(cancelled(request));
            }
            if (running != null && running.key.equals(key) && running.session.equals(session)) {
                return running.future;
            }
            Entry existing = pending.get(key);
            if (existing != null && existing.session.equals(session)) {
                if (safePriority.rank() < existing.priority.rank()) {
                    queue.remove(existing);
                    existing.priority = safePriority;
                    queue.add(existing);
                    publishLocked("Prioridad elevada para página " + key.pageNumber() + ".");
                }
                return existing.future;
            }
            if (existing != null) {
                queue.remove(existing);
                pending.remove(key);
                existing.cancelled.set(true);
                existing.future.complete(cancelled(existing.request));
            }
            Entry entry = new Entry(key, safePriority, session, request, ++sequence);
            pending.put(key, entry);
            queue.add(entry);
            requested++;
            monitor.notifyAll();
            publishLocked("Página " + key.pageNumber() + " en cola.");
            return entry.future;
        }
    }

    /** Promotes existing work only. It never creates a queue item or retries a terminal page. */
    public boolean reprioritizePending(PdfPreparationTaskKey key,
                                       PdfPreparationPriority priority) {
        Objects.requireNonNull(key, "key");
        PdfPreparationPriority safe = priority == null
                ? PdfPreparationPriority.NORMAL : priority;
        synchronized (monitor) {
            Entry existing = pending.get(key);
            if (existing == null) return false;
            if (safe.rank() < existing.priority.rank()) {
                queue.remove(existing);
                existing.priority = safe;
                queue.add(existing);
                publishLocked("Prioridad elevada para página "
                        + key.pageNumber() + ".");
            }
            return true;
        }
    }

    public void pause() {
        synchronized (monitor) {
            paused = true;
            publishLocked("Preparación PDF pausada.");
        }
    }

    public void resume() {
        synchronized (monitor) {
            paused = false;
            monitor.notifyAll();
            publishLocked("Preparación PDF reanudada.");
        }
    }

    public void cancelPending(PdfProjectSessionToken session) {
        if (session == null) return;
        List<Entry> cancelled = new ArrayList<>();
        synchronized (monitor) {
            queue.removeIf(entry -> {
                if (entry.session.equals(session)) {
                    pending.remove(entry.key);
                    cancelled.add(entry);
                    return true;
                }
                return false;
            });
            publishLocked("Preparaciones pendientes canceladas.");
        }
        cancelled.forEach(entry -> entry.future.complete(cancelled(entry.request)));
    }

    /**
     * Cancels every page that has not started yet, regardless of the viewport
     * session that requested it. The page currently being prepared is allowed
     * to finish at its next safe publication boundary.
     *
     * <p>This is the application-level action exposed by the global document
     * preparation control. Session-specific cancellation remains available to
     * coordinators that are closing or replacing a project.</p>
     */
    public void cancelAllPending() {
        List<Entry> cancelled;
        synchronized (monitor) {
            cancelled = new ArrayList<>(queue);
            queue.clear();
            pending.clear();
            publishLocked("Preparaciones pendientes canceladas.");
        }
        cancelled.forEach(entry -> entry.future.complete(cancelled(entry.request)));
    }

    public void closeSession(PdfProjectSessionToken session) {
        if (session == null) return;
        session.invalidate();
        cancelPending(session);
        synchronized (monitor) {
            if (running != null && running.session.equals(session)) {
                running.cancelled.set(true);
            }
            monitor.notifyAll();
        }
    }

    public void addProgressListener(Consumer<PdfPreparationProgress> listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeProgressListener(Consumer<PdfPreparationProgress> listener) {
        listeners.remove(listener);
    }

    public PdfPreparationProgress snapshot() {
        synchronized (monitor) {
            return snapshotLocked("");
        }
    }

    @Override
    public void close() {
        List<Entry> abandoned;
        synchronized (monitor) {
            if (closed) return;
            closed = true;
            abandoned = new ArrayList<>(queue);
            queue.clear();
            pending.clear();
            if (running != null) running.cancelled.set(true);
            monitor.notifyAll();
        }
        abandoned.forEach(entry -> entry.future.complete(cancelled(entry.request)));
        thread.interrupt();
    }

    private void runLoop() {
        while (true) {
            Entry entry = awaitNext();
            if (entry == null) return;
            try {
                Entry first = entry;
                batchRunner.run(() -> drainAvailable(first));
            } catch (Exception failure) {
                Entry failed;
                synchronized (monitor) {
                    failed = running;
                }
                if (failed != null && !failed.future.isDone()) {
                    failed.future.completeExceptionally(failure);
                    finish(failed, false, "Fallo la pagina "
                            + failed.key.pageNumber() + ": "
                            + failure.getMessage());
                }
                if (failure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    if (closed) return;
                }
            }
        }
    }

    private Entry awaitNext() {
        synchronized (monitor) {
            while (!closed && (paused || queue.isEmpty())) {
                try {
                    monitor.wait();
                } catch (InterruptedException ignored) {
                    if (closed) return null;
                }
            }
            if (closed) return null;
            Entry entry = queue.poll();
            pending.remove(entry.key);
            running = entry;
            publishLocked("Preparando pagina " + entry.key.pageNumber() + ".");
            return entry;
        }
    }

    private void drainAvailable(Entry first) {
        Entry entry = first;
        while (entry != null) {
            PreparePdfPageResult result;
            try {
                Entry current = entry;
                PdfPreparationCancellationToken token =
                        () -> current.cancelled.get()
                                || !current.session.active() || closed;
                PreparePdfPageRequest request = new PreparePdfPageRequest(
                        current.request.workspace(), current.request.cacheDirectory(),
                        current.request.pageNumber(), current.request.forceOcr(), token,
                        current.priority, current.request.progress(),
                        current.request.origin(), current.request.retryAllowed(),
                        current.request.scopeId(), current.request.readingStrategy(), current.request.nativeTextProvider());
                result = worker.prepare(request, current.priority);
            } catch (RuntimeException failure) {
                boolean wasCancelled = failure instanceof PdfPreparationCancelledException
                        || entry.cancelled.get() || !entry.session.active() || closed;
                result = wasCancelled
                        ? PreparePdfPageResult.cancelled(entry.key.pageNumber(), null)
                        : PreparePdfPageResult.technicalFailure(
                                entry.key.pageNumber(), null, List.of(),
                                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                                        .PdfOperationAttemptState.FAILED,
                                "UNEXPECTED_RUNTIME_FAILURE",
                                "No se pudo procesar la página "
                                        + entry.key.pageNumber() + ".",
                                failure);
                entry.future.complete(result);
                finish(entry, false, result.terminalMessage());
                entry = nextAvailable();
                continue;
            }
            entry.future.complete(result);
            finish(entry, result.succeeded(), result.terminalMessage());
            entry = nextAvailable();
        }
    }

    private Entry nextAvailable() {
        synchronized (monitor) {
            if (closed || paused || queue.isEmpty()) return null;
            Entry entry = queue.poll();
            pending.remove(entry.key);
            running = entry;
            publishLocked("Preparando pagina " + entry.key.pageNumber() + ".");
            return entry;
        }
    }

    private void finish(Entry entry, boolean success, String message) {
        synchronized (monitor) {
            if (running == entry) running = null;
            // Work completion is independent from semantic acceptance. Every
            // terminal page advances scheduler progress exactly once.
            completed++;
            publishLocked(message);
            monitor.notifyAll();
        }
    }

    private void publishLocked(String message) {
        PdfPreparationProgress progress = snapshotLocked(message);
        listeners.forEach(listener -> {
            try {
                listener.accept(progress);
            } catch (RuntimeException ignored) {
                // A presentation listener must never stop the worker.
            }
        });
    }

    private PdfPreparationProgress snapshotLocked(String message) {
        PdfPreparationProgress.State state = closed
                ? PdfPreparationProgress.State.CANCELLED
                : paused ? PdfPreparationProgress.State.PAUSED
                : running != null ? PdfPreparationProgress.State.RUNNING
                : PdfPreparationProgress.State.IDLE;
        return new PdfPreparationProgress(state,
                running == null ? 0 : running.key.pageNumber(),
                completed, requested, queue.size(), message);
    }

    private static PreparePdfPageResult cancelled(PreparePdfPageRequest request) {
        return PreparePdfPageResult.cancelled(request.pageNumber(), null);
    }

    private static final class Entry {
        private final PdfPreparationTaskKey key;
        private PdfPreparationPriority priority;
        private final PdfProjectSessionToken session;
        private final PreparePdfPageRequest request;
        private final long sequence;
        private final CompletableFuture<PreparePdfPageResult> future = new CompletableFuture<>();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private Entry(PdfPreparationTaskKey key,
                      PdfPreparationPriority priority,
                      PdfProjectSessionToken session,
                      PreparePdfPageRequest request,
                      long sequence) {
            this.key = key;
            this.priority = priority;
            this.session = session;
            this.request = request;
            this.sequence = sequence;
        }
    }
}
