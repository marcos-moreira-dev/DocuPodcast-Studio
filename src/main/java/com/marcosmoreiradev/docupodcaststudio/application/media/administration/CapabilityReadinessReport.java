package com.marcosmoreiradev.docupodcaststudio.application.media.administration;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineReadiness;

import java.util.List;
import java.util.Map;

/** Readiness of the requested operation, distinct from an action result. */
public record CapabilityReadinessReport(
        CapabilityRequirement requirement,
        CapabilityReadinessState state,
        String summary,
        List<String> issues,
        List<String> recommendedActions,
        String technicalDetails,
        Map<String, String> diagnostics) {
    public CapabilityReadinessReport {
        state = state == null ? CapabilityReadinessState.INVALID : state;
        summary = summary == null ? "" : summary.strip();
        issues = issues == null ? List.of() : List.copyOf(issues);
        recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
        technicalDetails = technicalDetails == null ? "" : technicalDetails.strip();
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public boolean ready() {
        return state == CapabilityReadinessState.READY;
    }

    static CapabilityReadinessReport from(CapabilityRequirement requirement, EngineReadiness readiness) {
        String searchable = (readiness.summary() + " " + readiness.diagnostics() + " "
                + String.join(" ", readiness.issues())).toLowerCase(java.util.Locale.ROOT);
        CapabilityReadinessState state;
        if (readiness.ready()) {
            state = CapabilityReadinessState.READY;
        } else if (readiness.state()
                == com.marcosmoreiradev.docupodcaststudio.media.api.ReadinessState.DEGRADED) {
            state = CapabilityReadinessState.DEGRADED;
        } else if (searchable.contains("puerto") || searchable.contains("conflict")) {
            state = CapabilityReadinessState.CONFLICT;
        } else if (searchable.contains("no responde") || searchable.contains("inicia el runtime")
                || searchable.contains("no está iniciado") || searchable.contains("detenido")) {
            state = CapabilityReadinessState.STOPPED;
        } else if (searchable.contains("falta") || searchable.contains("missing")
                || searchable.contains("no existe")) {
            state = CapabilityReadinessState.MISSING;
        } else {
            state = CapabilityReadinessState.INVALID;
        }
        return new CapabilityReadinessReport(requirement, state, readiness.summary(), readiness.issues(),
                readiness.recommendedActions(), readiness.diagnostics(),
                Map.of("engineState", readiness.state().name()));
    }
}
