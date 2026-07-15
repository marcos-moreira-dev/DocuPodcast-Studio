package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Deadline-based runtime sequencer for continuous document playback.
 *
 * <p>It deliberately does not trust a single signal. Java Sound callbacks, JavaFX ticks and line
 * positions are useful diagnostics, but the reading must continue when the expected WAV duration
 * expires. This class owns the cue deadline outside the large shell view-model.</p>
 */
public final class PlaybackCueDeadlineSequencer implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "docupodcast-playback-deadline");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong generation = new AtomicLong();
    private final PlaybackTimingPolicy timing = PlaybackTimingPolicy.defaults();
    private ScheduledFuture<?> pollTask;
    private ScheduledFuture<?> deadlineTask;

    public void start(PlaybackCue cue,
                      double localOffsetSeconds,
                      double playbackRate,
                      BooleanSupplier pausedOrInactive,
                      BooleanSupplier transitionGuardActive,
                      BooleanSupplier playerPlaying,
                      DoubleSupplier localPosition,
                      Consumer<PlaybackCue> completed) {
        stop();
        if (cue == null || completed == null) {
            return;
        }
        long id = generation.incrementAndGet();
        long deadlineMs = timing.deadlineDelayMillis(cue.durationSeconds(), localOffsetSeconds, playbackRate);
        Runnable finish = () -> finishIfCurrent(id, cue, pausedOrInactive, completed);
        pollTask = executor.scheduleAtFixedRate(() -> {
            if (id != generation.get() || get(pausedOrInactive) || get(transitionGuardActive)) {
                return;
            }
            boolean stopped = !get(playerPlaying);
            double position = safePosition(localPosition);
            boolean reachedEnd = position >= 0.0 && position + timing.completionGraceSeconds() >= cue.durationSeconds();
            boolean stoppedNearEnd = stopped
                    && position >= 0.0
                    && position + Math.max(0.30, timing.completionGraceSeconds()) >= cue.durationSeconds();
            if (stoppedNearEnd || reachedEnd) {
                finish.run();
            }
        }, timing.pollIntervalMillis(), timing.pollIntervalMillis(), TimeUnit.MILLISECONDS);
        deadlineTask = executor.schedule(finish, deadlineMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        generation.incrementAndGet();
        if (pollTask != null) { pollTask.cancel(false); pollTask = null; }
        if (deadlineTask != null) { deadlineTask.cancel(false); deadlineTask = null; }
    }

    @Override
    public void close() {
        stop();
        executor.shutdownNow();
    }

    private void finishIfCurrent(long id, PlaybackCue cue, BooleanSupplier pausedOrInactive, Consumer<PlaybackCue> completed) {
        if (id != generation.get() || get(pausedOrInactive)) {
            return;
        }
        long completedGeneration = generation.incrementAndGet();
        if (pollTask != null) { pollTask.cancel(false); pollTask = null; }
        if (deadlineTask != null) { deadlineTask.cancel(false); deadlineTask = null; }
        Platform.runLater(() -> {
            if (generation.get() == completedGeneration) {
                completed.accept(cue);
            }
        });
    }

    private static boolean get(BooleanSupplier supplier) {
        return supplier != null && supplier.getAsBoolean();
    }

    private static double safePosition(DoubleSupplier supplier) {
        if (supplier == null) {
            return -1.0;
        }
        try {
            return supplier.getAsDouble();
        } catch (RuntimeException ex) {
            return -1.0;
        }
    }
}
