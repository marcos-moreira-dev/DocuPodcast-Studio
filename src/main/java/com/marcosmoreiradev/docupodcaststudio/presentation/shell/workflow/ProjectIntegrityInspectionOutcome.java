package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;

import java.util.Optional;

/** Presentation-facing result for manual or automatic project integrity inspection. */
public record ProjectIntegrityInspectionOutcome(
        String statusMessage,
        Optional<UserVisibleDecision> decision
) {
    public ProjectIntegrityInspectionOutcome {
        statusMessage = statusMessage == null || statusMessage.isBlank()
                ? "Integridad del proyecto revisada."
                : statusMessage.strip();
        decision = decision == null ? Optional.empty() : decision;
    }
}
