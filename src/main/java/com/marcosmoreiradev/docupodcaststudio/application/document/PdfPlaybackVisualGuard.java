package com.marcosmoreiradev.docupodcaststudio.application.document;

/** Rejects late PDF paint events that no longer belong to the active playback transition. */
public final class PdfPlaybackVisualGuard {
    private long sequence;
    private String expectedRegionId = "";
    private String paintedRegionId = "";

    public synchronized boolean request(long nextSequence, String regionId) {
        String normalized = normalize(regionId);
        if (nextSequence < sequence || normalized.isBlank()) return false;
        sequence = nextSequence;
        expectedRegionId = normalized;
        paintedRegionId = "";
        return true;
    }

    public synchronized boolean painted(long eventSequence, String regionId) {
        String normalized = normalize(regionId);
        if (eventSequence != sequence || normalized.isBlank()) return false;
        paintedRegionId = normalized;
        return expectedRegionId.equals(paintedRegionId);
    }

    public synchronized void clear() {
        sequence++;
        expectedRegionId = "";
        paintedRegionId = "";
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(sequence, expectedRegionId, paintedRegionId,
                !expectedRegionId.isBlank() && expectedRegionId.equals(paintedRegionId));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public record Snapshot(long sequence, String expectedRegionId,
                           String paintedRegionId, boolean identityMatches) { }
}
