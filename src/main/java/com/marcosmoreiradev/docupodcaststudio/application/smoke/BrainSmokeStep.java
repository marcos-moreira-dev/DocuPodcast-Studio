package com.marcosmoreiradev.docupodcaststudio.application.smoke;

import java.time.Duration;
import java.util.Objects;

/** Immutable evidence row for a single core-smoke operation. */
public record BrainSmokeStep(
        String id,
        String name,
        BrainSmokeStepStatus status,
        String detail,
        Duration duration
) {
    public BrainSmokeStep {
        id = normalize(id, "STEP");
        name = normalize(name, id);
        status = Objects.requireNonNullElse(status, BrainSmokeStepStatus.FAILED);
        detail = normalize(detail, "Sin detalle");
        duration = duration == null || duration.isNegative() ? Duration.ZERO : duration;
    }

    public static BrainSmokeStep passed(String id, String name, String detail, Duration duration) {
        return new BrainSmokeStep(id, name, BrainSmokeStepStatus.PASSED, detail, duration);
    }

    public static BrainSmokeStep warning(String id, String name, String detail, Duration duration) {
        return new BrainSmokeStep(id, name, BrainSmokeStepStatus.WARNING, detail, duration);
    }

    public static BrainSmokeStep failed(String id, String name, String detail, Duration duration) {
        return new BrainSmokeStep(id, name, BrainSmokeStepStatus.FAILED, detail, duration);
    }

    public String durationLabel() {
        long millis = Math.max(0, duration.toMillis());
        if (millis < 1_000) {
            return millis + " ms";
        }
        return String.format(java.util.Locale.ROOT, "%.2f s", millis / 1_000.0);
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
