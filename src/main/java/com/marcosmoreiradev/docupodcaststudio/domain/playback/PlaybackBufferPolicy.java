package com.marcosmoreiradev.docupodcaststudio.domain.playback;

/**
 * User-facing buffer policy for narrated-document playback.
 *
 * <p>The policy lets the reader start once a small number of sentence/segment chunks is ready,
 * while the audio factory continues preparing more chunks in the background. It is intentionally
 * small and engine-agnostic so settings, playback and tests can share one contract.</p>
 */
public record PlaybackBufferPolicy(
        int initialReadySegments,
        int lookaheadSegments,
        boolean pauseWhenBufferMissing
) {
    public PlaybackBufferPolicy {
        initialReadySegments = Math.max(1, initialReadySegments);
        lookaheadSegments = Math.max(initialReadySegments, lookaheadSegments);
    }

    public static PlaybackBufferPolicy defaultPolicy() {
        return new PlaybackBufferPolicy(5, 10, true);
    }

    public int initialTargetFor(int totalSegments) {
        int total = Math.max(0, totalSegments);
        if (total == 0) {
            return initialReadySegments;
        }
        return Math.min(initialReadySegments, total);
    }

    public int lookaheadTargetAfter(int currentSegmentIndex, int totalSegments) {
        int total = Math.max(0, totalSegments);
        if (total == 0) {
            return lookaheadSegments;
        }
        int current = Math.max(0, currentSegmentIndex);
        return Math.min(total, current + 1 + lookaheadSegments);
    }

    public boolean isSegmentReady(int zeroBasedSegmentIndex, int completedSegments, int totalSegments) {
        int total = Math.max(0, totalSegments);
        int index = Math.max(0, zeroBasedSegmentIndex);
        if (total == 0 || index >= total) {
            return false;
        }
        return Math.max(0, completedSegments) > index;
    }

    public boolean canStart(int completedSegments, int totalSegments) {
        int completed = Math.max(0, completedSegments);
        int total = Math.max(0, totalSegments);
        if (total == 0) {
            return false;
        }
        return completed >= initialTargetFor(total);
    }

    public boolean shouldWaitForNextChunk(int currentSegmentIndex, int completedSegments, int totalSegments) {
        int total = Math.max(0, totalSegments);
        int next = Math.max(0, currentSegmentIndex) + 1;
        return pauseWhenBufferMissing && total > 0 && next < total && !isSegmentReady(next, completedSegments, totalSegments);
    }

    public String userLabel() {
        return "Buffer: inicia con " + initialReadySegments
                + " fragmentos y mantiene hasta " + lookaheadSegments + " por delante.";
    }

    public String waitingLabel() {
        return "Preparando el siguiente fragmento de audio; la lectura continuará automáticamente.";
    }
}
