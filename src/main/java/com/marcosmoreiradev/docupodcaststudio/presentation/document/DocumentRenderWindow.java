package com.marcosmoreiradev.docupodcaststudio.presentation.document;

/**
 * Small immutable projection for rendering a bounded slice of a large document.
 * The document model stays complete; only the JavaFX nodes are windowed.
 */
public record DocumentRenderWindow(int startInclusive, int endExclusive, int totalBlocks) {
    public DocumentRenderWindow {
        totalBlocks = Math.max(0, totalBlocks);
        startInclusive = Math.max(0, Math.min(startInclusive, totalBlocks));
        endExclusive = Math.max(startInclusive, Math.min(endExclusive, totalBlocks));
    }

    public static DocumentRenderWindow all(int totalBlocks) {
        return new DocumentRenderWindow(0, Math.max(0, totalBlocks), totalBlocks);
    }

    public static DocumentRenderWindow around(int totalBlocks, int pivotIndex, int windowSize) {
        int safeTotal = Math.max(0, totalBlocks);
        if (safeTotal == 0) {
            return all(0);
        }
        int safeWindow = Math.max(1, Math.min(windowSize, safeTotal));
        int safePivot = Math.max(0, Math.min(pivotIndex, safeTotal - 1));
        int half = Math.max(0, safeWindow / 2);
        int start = Math.max(0, safePivot - half);
        int end = Math.min(safeTotal, start + safeWindow);
        if (end - start < safeWindow) {
            start = Math.max(0, end - safeWindow);
        }
        return new DocumentRenderWindow(start, end, safeTotal);
    }

    public DocumentRenderWindow next(int windowSize) {
        if (!hasNext()) {
            return this;
        }
        return around(totalBlocks, Math.min(totalBlocks - 1, endExclusive), windowSize);
    }

    public DocumentRenderWindow previous(int windowSize) {
        if (!hasPrevious()) {
            return this;
        }
        return around(totalBlocks, Math.max(0, startInclusive - 1), windowSize);
    }

    public boolean hasPrevious() {
        return startInclusive > 0;
    }

    public boolean hasNext() {
        return endExclusive < totalBlocks;
    }

    public int visibleCount() {
        return Math.max(0, endExclusive - startInclusive);
    }

    public String humanRangeLabel() {
        if (totalBlocks == 0 || visibleCount() == 0) {
            return "Sin bloques visibles";
        }
        return "Bloques " + (startInclusive + 1) + "–" + endExclusive + " de " + totalBlocks;
    }
}
