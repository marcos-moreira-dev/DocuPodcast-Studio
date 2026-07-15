package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.util.List;

/** Final migration guardrail result derived from source and architecture contracts. */
public record ArchitectureGuardrailReport(
        int viewModelLines,
        boolean viewModelWithinBudget,
        boolean storyboardHidden,
        boolean audioJobsHidden,
        boolean officialModesPresent,
        boolean fragmentIdPresent,
        boolean exportCenterPresent,
        boolean noBannedScopeIntroduced,
        List<String> checklistItems,
        List<String> issues
) {
    public ArchitectureGuardrailReport {
        viewModelLines = Math.max(0, viewModelLines);
        checklistItems = checklistItems == null ? List.of() : List.copyOf(checklistItems);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean passed() {
        return viewModelWithinBudget
                && storyboardHidden
                && audioJobsHidden
                && officialModesPresent
                && fragmentIdPresent
                && exportCenterPresent
                && noBannedScopeIntroduced
                && issues.isEmpty();
    }
}
