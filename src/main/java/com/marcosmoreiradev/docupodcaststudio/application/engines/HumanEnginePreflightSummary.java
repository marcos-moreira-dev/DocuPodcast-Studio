package com.marcosmoreiradev.docupodcaststudio.application.engines;

import java.util.List;

/** Compact, user-facing preflight summary for Settings and startup prompts. */
public record HumanEnginePreflightSummary(
        HumanEnginePreflightState state,
        String headline,
        String primaryAction,
        String statusBarLabel,
        List<String> details
) {
    public HumanEnginePreflightSummary {
        state = state == null ? HumanEnginePreflightState.REQUIRES_PREPARATION : state;
        headline = normalize(headline);
        primaryAction = normalize(primaryAction);
        statusBarLabel = normalize(statusBarLabel);
        details = List.copyOf(details == null ? List.of() : details);
    }

    public boolean ready() {
        return state == HumanEnginePreflightState.READY;
    }

    public boolean requiresPreparation() {
        return state == HumanEnginePreflightState.REQUIRES_PREPARATION;
    }

    public boolean error() {
        return state == HumanEnginePreflightState.ERROR;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
