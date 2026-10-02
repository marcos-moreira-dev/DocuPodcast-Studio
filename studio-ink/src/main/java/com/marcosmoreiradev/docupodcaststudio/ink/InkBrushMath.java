package com.marcosmoreiradev.docupodcaststudio.ink;

/** Shared brush math for live ink preview and persisted vector replay. */
public final class InkBrushMath {
    private InkBrushMath() {
    }

    public static double safeMaxWidth(double width) {
        return Double.isFinite(width) ? Math.max(1.0, width) : 1.0;
    }

    public static double safePressure(double pressure) {
        return Double.isFinite(pressure) ? Math.max(0.0, Math.min(1.0, pressure)) : 1.0;
    }

    public static double pressureWidth(double maxWidth, double pressure) {
        double safeMax = safeMaxWidth(maxWidth);
        double safePressure = safePressure(pressure);
        return safeMax * safePressure;
    }
}
