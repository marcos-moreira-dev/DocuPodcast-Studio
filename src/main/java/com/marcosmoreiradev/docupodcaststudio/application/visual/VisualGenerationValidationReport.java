package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.List;

/** Optional consumer-selected validation result for a generated visual. */
public record VisualGenerationValidationReport(boolean valid, List<String> issues) {
    public VisualGenerationValidationReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
        valid = valid && issues.isEmpty();
    }
}
