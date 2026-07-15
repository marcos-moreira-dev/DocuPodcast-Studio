package com.marcosmoreiradev.docupodcaststudio.presentation.document;

/**
 * Presentation policy for keeping the currently narrated block in a stable reading band.
 *
 * <p>The main document surface should not chase the active sentence with abrupt top/bottom
 * jumps. This policy keeps the active block a little above the visual center so the user sees
 * some context before and after the sentence being read.</p>
 */
public final class ActiveReadingAnchor {
    private final double upperComfortBand;

    private ActiveReadingAnchor(double upperComfortBand) {
        this.upperComfortBand = clamp(upperComfortBand);
    }

    public static ActiveReadingAnchor defaultAnchor() {
        return new ActiveReadingAnchor(0.34);
    }

    public double upperComfortBand() {
        return upperComfortBand;
    }

    public double scrollValueFor(int blockIndex, int blockCount) {
        if (blockCount <= 1 || blockIndex <= 0) {
            return 0.0;
        }
        double denominator = Math.max(1.0, blockCount - 1.0);
        double rawPosition = blockIndex / denominator;
        double adjusted = rawPosition - upperComfortBand / Math.max(1.0, blockCount / 3.0);
        return clamp(adjusted);
    }

    public String userLabel() {
        return "Seguimiento activo: el segmento narrado se mantiene en una zona estable de lectura.";
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
