package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Performs bounded, idempotent shutdown in a deterministic order. */
public final class ApplicationLifecycleCoordinator implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationLifecycleCoordinator.class);
    private final Duration timeout;
    private final List<LifecycleParticipant> participants;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ApplicationLifecycleCoordinator(Duration timeout, List<LifecycleParticipant> participants) {
        this.timeout = timeout == null || timeout.isNegative() ? Duration.ofSeconds(5) : timeout;
        this.participants = participants == null ? List.of() : List.copyOf(participants);
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        long deadline = System.nanoTime() + timeout.toNanos();
        ArrayList<Throwable> failures = new ArrayList<>();
        participants.forEach(participant -> attempt(participant::stopAcceptingWork, failures));
        participants.forEach(participant -> attempt(participant::requestCancellation, failures));
        for (LifecycleParticipant participant : participants) {
            long remaining = Math.max(0, deadline - System.nanoTime());
            try {
                if (!participant.awaitTermination(Duration.ofNanos(remaining))) {
                    LOGGER.warn("Lifecycle participant did not terminate before timeout: {}",
                            participant.getClass().getSimpleName());
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                failures.add(interrupted);
                break;
            } catch (RuntimeException failure) {
                failures.add(failure);
            }
        }
        participants.forEach(participant -> attempt(participant::persistState, failures));
        ArrayList<LifecycleParticipant> reverse = new ArrayList<>(participants);
        Collections.reverse(reverse);
        reverse.forEach(participant -> attempt(participant::close, failures));
        if (failures.isEmpty()) LOGGER.info("Application lifecycle closed cleanly");
        else LOGGER.warn("Application lifecycle closed with {} failure(s)", failures.size(), failures.getFirst());
    }

    private static void attempt(CheckedAction action, List<Throwable> failures) {
        try { action.run(); } catch (Throwable failure) { failures.add(failure); }
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
}
