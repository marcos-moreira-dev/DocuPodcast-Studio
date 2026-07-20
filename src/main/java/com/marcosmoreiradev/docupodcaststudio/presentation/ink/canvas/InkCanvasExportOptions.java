package com.marcosmoreiradev.docupodcaststudio.presentation.ink.canvas;

/** Export policy for a technical-problem canvas snapshot. */
public record InkCanvasExportOptions(
        int preferredScale,
        long maxPixelCount,
        boolean cropToContent,
        boolean fullLogicalCanvas,
        double margin,
        double minWidth,
        double minHeight
) {
    public InkCanvasExportOptions(
            int preferredScale,
            long maxPixelCount,
            boolean cropToContent,
            double margin,
            double minWidth,
            double minHeight
    ) {
        this(preferredScale, maxPixelCount, cropToContent, false, margin, minWidth, minHeight);
    }

    public InkCanvasExportOptions {
        preferredScale = Math.max(1, preferredScale);
        maxPixelCount = Math.max(1_000_000L, maxPixelCount);
        fullLogicalCanvas = fullLogicalCanvas && !cropToContent;
        margin = finiteOrDefault(margin, 0.0);
        minWidth = Math.max(1.0, finiteOrDefault(minWidth, 1.0));
        minHeight = Math.max(1.0, finiteOrDefault(minHeight, 1.0));
    }

    public static InkCanvasExportOptions internalPersistence() {
        return new InkCanvasExportOptions(1, 24_000_000L, true, 100, 720, 520);
    }

    public static InkCanvasExportOptions premiumExternal() {
        return new InkCanvasExportOptions(4, 128_000_000L, false, true, 0, 720, 520);
    }

    public static InkCanvasExportOptions undoSnapshot() {
        return new InkCanvasExportOptions(1, 48_000_000L, false, 100, 720, 520);
    }

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.0, value) : fallback;
    }
}
