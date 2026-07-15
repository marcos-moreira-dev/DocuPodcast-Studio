package com.marcosmoreiradev.docupodcaststudio.domain.playback;

/**
 * User-facing streaming buffer state for narrated-document playback.
 *
 * <p>The window is intentionally independent from JavaFX and from any concrete TTS engine. It
 * answers the questions that matter to the reader: can playback start, can it continue at the next
 * fragment, and how far ahead should the background audio factory keep preparing chunks?</p>
 */
public record StreamingPlaybackWindow(
        int completedSegments,
        int totalSegments,
        int currentSegmentIndex,
        int initialReadyTarget,
        int lookaheadTarget,
        boolean readyToStart,
        boolean currentSegmentReady,
        boolean nextSegmentReady,
        boolean waitingForNextChunk
) {
    public StreamingPlaybackWindow {
        completedSegments = Math.max(0, completedSegments);
        totalSegments = Math.max(0, totalSegments);
        currentSegmentIndex = Math.max(0, currentSegmentIndex);
        initialReadyTarget = Math.max(1, Math.min(Math.max(1, totalSegments), initialReadyTarget));
        lookaheadTarget = Math.max(initialReadyTarget, Math.min(Math.max(1, totalSegments), lookaheadTarget));
    }

    public static StreamingPlaybackWindow of(
            PlaybackBufferPolicy policy,
            int completedSegments,
            int totalSegments,
            int currentSegmentIndex
    ) {
        PlaybackBufferPolicy safePolicy = policy == null ? PlaybackBufferPolicy.defaultPolicy() : policy;
        int safeTotal = Math.max(0, totalSegments);
        int safeCompleted = Math.max(0, completedSegments);
        int safeIndex = Math.max(0, currentSegmentIndex);
        boolean readyToStart = safePolicy.canStart(safeCompleted, safeTotal);
        boolean currentReady = safePolicy.isSegmentReady(safeIndex, safeCompleted, safeTotal);
        boolean nextReady = safePolicy.isSegmentReady(safeIndex + 1, safeCompleted, safeTotal);
        boolean waiting = safeTotal > 0 && readyToStart && !nextReady && safeIndex + 1 < safeTotal;
        return new StreamingPlaybackWindow(
                safeCompleted,
                safeTotal,
                safeIndex,
                safePolicy.initialTargetFor(safeTotal),
                safePolicy.lookaheadTargetAfter(safeIndex, safeTotal),
                readyToStart,
                currentReady,
                nextReady,
                waiting);
    }

    public int remainingSegments() {
        return Math.max(0, totalSegments - completedSegments);
    }

    public boolean completedAll() {
        return totalSegments > 0 && completedSegments >= totalSegments;
    }

    public String readerStatusLabel() {
        if (totalSegments <= 0) {
            return "Sin fragmentos de audio para preparar.";
        }
        if (!readyToStart) {
            return "Preparando buffer inicial: " + completedSegments + "/" + initialReadyTarget + " fragmentos.";
        }
        if (waitingForNextChunk) {
            return "Cargando el siguiente fragmento. La lectura continuará automáticamente.";
        }
        if (completedAll()) {
            return "Todo el documento está preparado para reproducirse.";
        }
        return "Reproduciendo con buffer: " + completedSegments + "/" + totalSegments
                + " fragmentos listos; objetivo cercano " + lookaheadTarget + ".";
    }
}
