package com.marcosmoreiradev.docupodcaststudio.application.ink;

/** Immutable point captured by the ink engine in logical canvas coordinates. */
public record InkPoint(double x, double y, long nanos, double pressure) {
    public InkPoint {
        x = finiteOrZero(x);
        y = finiteOrZero(y);
        pressure = Double.isFinite(pressure) ? Math.max(0.0, Math.min(1.0, pressure)) : 1.0;
    }

    public static InkPoint of(double x, double y, long nanos) {
        return new InkPoint(x, y, nanos, 1.0);
    }

    public static InkPoint of(double x, double y, long nanos, double pressure) {
        return new InkPoint(x, y, nanos, pressure);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
