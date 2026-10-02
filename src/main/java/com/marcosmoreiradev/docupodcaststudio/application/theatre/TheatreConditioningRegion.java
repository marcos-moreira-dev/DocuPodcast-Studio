package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.Locale;
import java.util.Map;

/** Normalized, provider-neutral influence region for one visual subject. */
public record TheatreConditioningRegion(double x, double y, double width, double height) {
    public TheatreConditioningRegion {
        x = clamp(x);
        y = clamp(y);
        width = Math.max(0.05, Math.min(1.0 - x, width));
        height = Math.max(0.05, Math.min(1.0 - y, height));
    }

    public Map<String, String> metadata() {
        return Map.of(
                "regionX", decimal(x),
                "regionY", decimal(y),
                "regionWidth", decimal(width),
                "regionHeight", decimal(height));
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(0.95, value)) : 0.0;
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
