package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Shared queue/retry/timeout/cancellation/progress coordinator for every media capability. */
public final class GenerationJobService implements AutoCloseable {
    private final GenerationJobRepository repository;
    private final ExecutorService workers;
    private final ScheduledExecutorService timers;
    private final Map<GenerationJobId, Control> running = new ConcurrentHashMap<>();

    public GenerationJobService(GenerationJobRepository repository, int concurrency) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.workers = Executors.newFixedThreadPool(Math.max(1, concurrency));
        this.timers = Executors.newSingleThreadScheduledExecutor();
    }

    public GenerationJobSnapshot submit(GenerationJobRequest request,
                                        GenerationJobOperation operation,
                                        ExecutionPolicy policy,
                                        ResourceLease lease) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(operation, "operation");
        if (running.containsKey(request.jobId())) throw new IllegalStateException("job already running: " + request.jobId());
        ExecutionPolicy safePolicy = Objects.requireNonNullElse(policy, ExecutionPolicy.defaults());
        ResourceLease safeLease = Objects.requireNonNullElse(lease, ResourceLease.NONE);
        GenerationJobSnapshot queued = GenerationJobSnapshot.queued(request);
        repository.save(queued);
        Control control = new Control();
        running.put(request.jobId(), control);
        control.future = workers.submit(() -> run(operation, safePolicy, safeLease, control, queued));
        control.timeoutFuture = timers.schedule(() -> {
            if (running.get(request.jobId()) != control) return;
            control.timedOut.set(true);
            control.cancelled.set(true);
            repository.find(request.jobId()).ifPresent(current -> repository.save(new GenerationJobSnapshot(
                    current.request(), GenerationJobStatus.FAILED, "timeout", current.progress(),
                    "Execution timed out after " + safePolicy.timeout(), List.of(),
                    "Execution timed out after " + safePolicy.timeout(), current.attempt(), Instant.now())));
            Future<?> future = control.future;
            if (future != null) future.cancel(true);
        }, safePolicy.timeout().toMillis(), TimeUnit.MILLISECONDS);
        return queued;
    }

    public GenerationJobSnapshot resume(GenerationJobId id,
                                        GenerationJobOperation operation,
                                        ExecutionPolicy policy,
                                        ResourceLease lease) {
        GenerationJobSnapshot previous = repository.find(id)
                .orElseThrow(() -> new IllegalArgumentException("unknown job: " + id));
        return submit(previous.request(), operation, policy, lease);
    }

    public boolean cancel(GenerationJobId id) {
        Control control = running.get(id);
        if (control == null) return false;
        control.cancelled.set(true);
        repository.find(id).ifPresent(current -> repository.save(new GenerationJobSnapshot(
                current.request(), GenerationJobStatus.CANCELLED, "cancelled", current.progress(),
                "Cancelled", List.of(), "Cancelled", current.attempt(), Instant.now())));
        Future<?> future = control.future;
        if (future != null) future.cancel(true);
        return true;
    }

    private void run(GenerationJobOperation operation,
                     ExecutionPolicy policy,
                     ResourceLease lease,
                     Control control,
                     GenerationJobSnapshot queued) {
        AtomicReference<GenerationJobSnapshot> current = new AtomicReference<>(queued);
        try (lease) {
            for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
                if (control.cancelled.get()) break;
                update(current, GenerationJobStatus.RUNNING, "running", current.get().progress(),
                        "", List.of(), "", attempt);
                int currentAttempt = attempt;
                ProgressSink progress = (stage, amount, message) -> update(current,
                        GenerationJobStatus.RUNNING, stage, amount, message, List.of(), "", currentAttempt);
                ExecutionContext context = new ExecutionContext(queued.request().jobId().value(),
                        control.cancelled::get, progress, policy, lease);
                try {
                    List<GenerationArtifact> artifacts = operation.execute(context);
                    if (!control.cancelled.get()) {
                        update(current, GenerationJobStatus.SUCCEEDED, "completed", 1, "",
                                artifacts, "", attempt);
                        return;
                    }
                } catch (Exception failure) {
                    if (control.cancelled.get()) break;
                    if (attempt == policy.maxAttempts()) {
                        update(current, GenerationJobStatus.FAILED, "failed", current.get().progress(),
                                failure.getMessage(), List.of(), failure.toString(), attempt);
                        return;
                    }
                    update(current, GenerationJobStatus.RUNNING, "retry", current.get().progress(),
                            failure.getMessage(), List.of(), failure.toString(), attempt);
                }
            }
            GenerationJobStatus status = control.timedOut.get()
                    ? GenerationJobStatus.FAILED : GenerationJobStatus.CANCELLED;
            String diagnostic = control.timedOut.get() ? "Execution timed out after " + policy.timeout() : "Cancelled";
            update(current, status, control.timedOut.get() ? "timeout" : "cancelled", current.get().progress(),
                    diagnostic, List.of(), diagnostic, current.get().attempt());
        } finally {
            Future<?> timeout = control.timeoutFuture;
            if (timeout != null) timeout.cancel(false);
            running.remove(queued.request().jobId(), control);
        }
    }

    private void update(AtomicReference<GenerationJobSnapshot> current,
                        GenerationJobStatus status,
                        String stage,
                        double progress,
                        String message,
                        List<GenerationArtifact> artifacts,
                        String diagnostic,
                        int attempt) {
        GenerationJobSnapshot snapshot = new GenerationJobSnapshot(current.get().request(), status, stage,
                progress, message, artifacts, diagnostic, attempt, Instant.now());
        current.set(snapshot);
        repository.save(snapshot);
    }

    @Override public void close() {
        running.keySet().forEach(this::cancel);
        workers.shutdownNow();
        timers.shutdownNow();
    }

    private static final class Control {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean timedOut = new AtomicBoolean();
        private volatile Future<?> future;
        private volatile Future<?> timeoutFuture;
    }
}
