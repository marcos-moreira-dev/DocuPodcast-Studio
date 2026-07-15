package com.marcosmoreiradev.docupodcaststudio.presentation.document;

/** Export policy for a technical-problem canvas snapshot. */
public record StudyProblemCanvasExportOptions(
        int preferredScale,
        long maxPixelCount,
        boolean cropToContent,
        boolean fullLogicalCanvas,
        double margin,
        double minWidth,
        double minHeight
) {
    public StudyProblemCanvasExportOptions(
            int preferredScale,
            long maxPixelCount,
            boolean cropToContent,
            double margin,
            double minWidth,
            double minHeight
    ) {
        this(preferredScale, maxPixelCount, cropToContent, false, margin, minWidth, minHeight);
    }

    public StudyProblemCanvasExportOptions {
        preferredScale = Math.max(1, preferredScale);
        maxPixelCount = Math.max(1_000_000L, maxPixelCount);
        fullLogicalCanvas = fullLogicalCanvas && !cropToContent;
        margin = finiteOrDefault(margin, 0.0);
        minWidth = Math.max(1.0, finiteOrDefault(minWidth, 1.0));
        minHeight = Math.max(1.0, finiteOrDefault(minHeight, 1.0));
    }

    public static StudyProblemCanvasExportOptions internalPersistence() {
        return new StudyProblemCanvasExportOptions(1, 24_000_000L, true, 100, 720, 520);
    }

    public static StudyProblemCanvasExportOptions premiumExternal() {
        return new StudyProblemCanvasExportOptions(4, 128_000_000L, false, true, 0, 720, 520);
    }

    public static StudyProblemCanvasExportOptions undoSnapshot() {
        return new StudyProblemCanvasExportOptions(1, 48_000_000L, false, 100, 720, 520);
    }

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.0, value) : fallback;
    }
}
