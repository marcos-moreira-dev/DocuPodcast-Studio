package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import javafx.application.Platform;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * Sequential cue driver for document read-aloud playback.
 *
 * <p>The queue owns cue order, but it no longer advances merely because a duration estimate has
 * elapsed. The real player completion callback is the preferred signal. The scheduled task acts as
 * a conservative watchdog and advances only after the player is no longer reporting sound.</p>
 */
public final class PlaybackSequentialQueueDriver implements AutoCloseable {
    @FunctionalInterface
    public interface CueStarter {
        boolean start(PlaybackCue cue);
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "docupodcast-playback-queue");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong generation = new AtomicLong();
    private final PlaybackTimingPolicy timing = PlaybackTimingPolicy.defaults();
    private ScheduledFuture<?> scheduledNext;
    private List<PlaybackCue> cues = List.of();
    private CueStarter cueStarter;
    private Runnable completed;
    private BooleanSupplier playerPlaying = () -> false;
    private int index = -1;
    private double playbackRate = 1.0;
    private boolean active;
    private boolean paused;
    private boolean pendingCueStart;
    private long pendingCueStartDelayMillis;
    private String acceptedCompletionUnitId = "";

    public void start(PlaybackManifest manifest, PlaybackCue startCue, double rate, CueStarter starter,
            BooleanSupplier playerPlaying, Runnable onCompleted) {
        stop();
        if (manifest == null || manifest.emptyManifest() || startCue == null || starter == null) {
            return;
        }
        cues = manifest.cues();
        index = indexOf(startCue);
        if (index < 0) {
            return;
        }
        cueStarter = starter;
        completed = onCompleted == null ? () -> { } : onCompleted;
        this.playerPlaying = playerPlaying == null ? () -> false : playerPlaying;
        playbackRate = timing.normalizeRate(rate);
        active = true;
        paused = false;
        generation.incrementAndGet();
        startCurrentCue(0.0);
    }

    public void refresh(PlaybackManifest manifest) {
        if (manifest == null || manifest.emptyManifest()) {
            return;
        }
        executor.execute(() -> refreshOnQueueThread(manifest.cues()));
    }

    public void stop() {
        generation.incrementAndGet();
        cancelScheduledNext();
        cues = List.of();
        cueStarter = null;
        completed = null;
        playerPlaying = () -> false;
        index = -1;
        active = false;
        paused = false;
        pendingCueStart = false;
        pendingCueStartDelayMillis = 0L;
        acceptedCompletionUnitId = "";
    }

    public void pause() {
        paused = true;
        cancelScheduledNext();
    }

    public void resume(double localOffsetSeconds) {
        if (!active || index < 0 || index >= cues.size()) {
            return;
        }
        paused = false;
        if (pendingCueStart) {
            scheduleStartCurrentCue(generation.get(), pendingCueStartDelayMillis);
            return;
        }
        scheduleWatchdog(cues.get(index), localOffsetSeconds);
    }

    public void setPlaybackRate(double rate, double localOffsetSeconds) {
        playbackRate = timing.normalizeRate(rate);
        if (!active || paused || index < 0 || index >= cues.size()) {
            return;
        }
        scheduleWatchdog(cues.get(index), localOffsetSeconds);
    }

    public boolean active() {
        return active;
    }

    public Optional<PlaybackCue> activeCue() {
        if (!active || index < 0 || index >= cues.size()) {
            return Optional.empty();
        }
        return Optional.of(cues.get(index));
    }

    public void advanceAfterCurrentCueFinished() {
        long id = generation.get();
        executor.execute(() -> advanceIfCurrent(id, true, ""));
    }

    public void advanceAfterCurrentCueFinished(String unitId) {
        long id = generation.get();
        executor.execute(() -> advanceIfCurrent(id, true, unitId == null ? "" : unitId));
    }

    public void attachToCurrentCue(PlaybackManifest manifest, PlaybackCue cue, double rate,
                                   double localOffsetSeconds, CueStarter starter,
                                   BooleanSupplier playerPlaying, Runnable onCompleted) {
        stop();
        if (manifest == null || manifest.emptyManifest() || cue == null || starter == null) return;
        cues = manifest.cues();
        index = indexOf(cue);
        if (index < 0) return;
        cueStarter = starter;
        completed = onCompleted == null ? () -> { } : onCompleted;
        this.playerPlaying = playerPlaying == null ? () -> false : playerPlaying;
        playbackRate = timing.normalizeRate(rate);
        active = true;
        paused = false;
        acceptedCompletionUnitId = "";
        generation.incrementAndGet();
        scheduleWatchdog(cue, Math.max(0.0, localOffsetSeconds));
    }

    public String diagnosticLabel() {
        if (!active || cues.isEmpty() || index < 0) {
            return "Secuencia de lectura: detenida.";
        }
        String next = index + 1 < cues.size() ? cues.get(index + 1).unitId() : "fin";
        return "Secuencia de lectura: " + (index + 1) + "/" + cues.size() + " · siguiente: " + next + ".";
    }

    @Override
    public void close() {
        stop();
        executor.shutdownNow();
    }

    private void startCurrentCue(double localOffsetSeconds) {
        long id = generation.get();
        Platform.runLater(() -> {
            if (!active || paused || id != generation.get() || index < 0 || index >= cues.size() || cueStarter == null) {
                return;
            }
            pendingCueStart = false;
            PlaybackCue cue = cues.get(index);
            boolean started = cueStarter.start(cue);
            if (!started) {
                stop();
                return;
            }
            scheduleWatchdog(cue, localOffsetSeconds);
        });
    }

    private void scheduleStartCurrentCue(long id) {
        scheduleStartCurrentCue(id, timing.interCuePauseMillis());
    }

    private void scheduleStartCurrentCue(long id, long delayMillis) {
        cancelScheduledNext();
        pendingCueStart = true;
        pendingCueStartDelayMillis = Math.max(0L, delayMillis);
        scheduledNext = executor.schedule(() -> {
            if (!active || paused || id != generation.get()) {
                return;
            }
            pendingCueStart = false;
            startCurrentCue(0.0);
        }, pendingCueStartDelayMillis, TimeUnit.MILLISECONDS);
    }

    private void scheduleWatchdog(PlaybackCue cue, double localOffsetSeconds) {
        cancelScheduledNext();
        if (!active || paused || cue == null) {
            return;
        }
        long id = generation.get();
        long delayMs = timing.watchdogDelayMillis(cue.durationSeconds(), localOffsetSeconds, playbackRate);
        scheduledNext = executor.schedule(() -> advanceIfCurrent(id, false, ""), delayMs, TimeUnit.MILLISECONDS);
    }

    private void rescheduleShortProbe(long id) {
        cancelScheduledNext();
        scheduledNext = executor.schedule(() -> advanceIfCurrent(id, false, ""), timing.shortProbeDelayMillis(), TimeUnit.MILLISECONDS);
    }

    private void advanceIfCurrent(long id, boolean fromPlayerCallback, String expectedUnitId) {
        if (!active || paused || id != generation.get()) {
            return;
        }
        String currentUnitId = cues.get(index).unitId();
        if (fromPlayerCallback && (!expectedUnitId.isBlank() && !expectedUnitId.equals(currentUnitId)
                || currentUnitId.equals(acceptedCompletionUnitId))) return;
        if (!fromPlayerCallback && safePlayerPlaying()) {
            rescheduleShortProbe(id);
            return;
        }
        acceptedCompletionUnitId = currentUnitId;
        cancelScheduledNext();
        PlaybackCue completedCue = cues.get(index);
        index++;
        if (index >= cues.size()) {
            Runnable done = completed == null ? () -> { } : completed;
            stop();
            Platform.runLater(done);
            return;
        }
        acceptedCompletionUnitId = "";
        long pauseMillis = timing.interCuePauseMillis(completedCue, cues.get(index), playbackRate);
        scheduleStartCurrentCue(id, pauseMillis);
    }

    private boolean safePlayerPlaying() {
        try {
            return playerPlaying != null && playerPlaying.getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private int indexOf(PlaybackCue cue) {
        return indexOf(cue, cues);
    }

    private static int indexOf(PlaybackCue cue, List<PlaybackCue> candidates) {
        if (cue == null || candidates == null) {
            return -1;
        }
        return indexOfUnit(cue.unitId(), candidates);
    }

    private static int indexOfUnit(String unitId, List<PlaybackCue> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).unitId().equals(unitId)) {
                return i;
            }
        }
        return -1;
    }

    private void refreshOnQueueThread(List<PlaybackCue> refreshedCues) {
        if (!active || refreshedCues == null || refreshedCues.isEmpty() || index < 0 || index >= cues.size()) {
            return;
        }
        String activeUnitId = cues.get(index).unitId();
        int refreshedIndex = indexOfUnit(activeUnitId, refreshedCues);
        if (refreshedIndex < 0) {
            return;
        }
        cues = List.copyOf(refreshedCues);
        index = refreshedIndex;
    }

    private void cancelScheduledNext() {
        if (scheduledNext != null) {
            scheduledNext.cancel(false);
            scheduledNext = null;
        }
    }

}
