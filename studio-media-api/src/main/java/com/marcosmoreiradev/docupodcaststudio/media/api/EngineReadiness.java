package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;
import java.util.Objects;

/** Honest engine state. Unavailable engines never silently fall back to a mock. */
public record EngineReadiness(
        EngineId engineId,
        ReadinessState state,
        String summary,
        List<String> issues,
        List<String> recommendedActions,
        String diagnostics) {
    public EngineReadiness {
        Objects.requireNonNull(engineId, "engineId");
        state = state == null ? ReadinessState.UNAVAILABLE : state;
        summary = summary == null ? "" : summary.strip();
        issues = issues == null ? List.of() : List.copyOf(issues);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
        diagnostics = diagnostics == null ? "" : diagnostics.strip();
    }

    public boolean ready() { return state == ReadinessState.READY; }

    public static EngineReadiness ready(EngineId id, String summary) {
        return new EngineReadiness(id, ReadinessState.READY, summary, List.of(), List.of(), "");
    }

    public static EngineReadiness unavailable(EngineId id, String summary, String action) {
        return new EngineReadiness(id, ReadinessState.UNAVAILABLE, summary,
                List.of(summary), action == null || action.isBlank() ? List.of() : List.of(action), "");
    }
}
