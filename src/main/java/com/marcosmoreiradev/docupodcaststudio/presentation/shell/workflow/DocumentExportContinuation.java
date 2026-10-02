package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.presentation.export.PreparedExportIntent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One-shot barrier between derivative repair and the requested export. */
public final class DocumentExportContinuation {
    public enum State { ACTIVE, EXPORT_STARTED, FAILED, CANCELLED }

    private final String correlationId;
    private final List<String> segmentIds;
    private final PreparedExportIntent intent;
    private final Runnable exportTask;
    private State state = State.ACTIVE;
    private int plannedTtsUnits;
    private int initiallyReusableTtsUnits;

    public DocumentExportContinuation(String correlationId, List<String> segmentIds,
                                      Runnable exportTask) {
        this(correlationId, segmentIds, null, exportTask);
    }

    public DocumentExportContinuation(String correlationId, List<String> segmentIds,
                                      PreparedExportIntent intent,
                                      Runnable exportTask) {
        this.correlationId = Objects.requireNonNullElse(correlationId, "").strip();
        this.segmentIds = List.copyOf(Objects.requireNonNullElse(segmentIds, List.of()));
        this.intent = intent;
        this.exportTask = Objects.requireNonNull(exportTask, "exportTask");
    }

    public String correlationId() { return correlationId; }
    public List<String> segmentIds() { return segmentIds; }
    public Optional<PreparedExportIntent> intent() { return Optional.ofNullable(intent); }
    public synchronized State state() { return state; }

    /** Records physical TTS units separately from parent narration segments. */
    public synchronized void recordAudioPlan(int totalUnits, int reusableUnits) {
        plannedTtsUnits = Math.max(0, totalUnits);
        initiallyReusableTtsUnits = Math.max(0,
                Math.min(plannedTtsUnits, reusableUnits));
    }

    public synchronized int plannedTtsUnits() { return plannedTtsUnits; }
    public synchronized int initiallyReusableTtsUnits() {
        return initiallyReusableTtsUnits;
    }

    /** Claims the export exactly once, and only after a fresh complete coverage result. */
    public synchronized Optional<Runnable> claimAfterCoverage(boolean complete) {
        if (state != State.ACTIVE) return Optional.empty();
        if (!complete) {
            state = State.FAILED;
            return Optional.empty();
        }
        state = State.EXPORT_STARTED;
        return Optional.of(exportTask);
    }

    public synchronized void fail() {
        if (state == State.ACTIVE) state = State.FAILED;
    }

    public synchronized void cancel() {
        if (state == State.ACTIVE) state = State.CANCELLED;
    }
}
