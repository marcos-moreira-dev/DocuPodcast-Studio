package com.marcosmoreiradev.docupodcaststudio.application.decisions;

import java.util.List;
import java.util.Objects;

/** Application result that can carry user-visible decisions without depending on presentation. */
public record OperationResult<T>(T value, List<UserVisibleDecision> decisions) {
    public OperationResult {
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
    }

    public static <T> OperationResult<T> success(T value) {
        return new OperationResult<>(value, List.of());
    }

    public static <T> OperationResult<T> withDecision(T value, UserVisibleDecision decision) {
        return new OperationResult<>(value, List.of(Objects.requireNonNull(decision, "decision")));
    }

    public OperationResult<T> plus(UserVisibleDecision decision) {
        if (decision == null) {
            return this;
        }
        java.util.ArrayList<UserVisibleDecision> next = new java.util.ArrayList<>(decisions);
        next.add(decision);
        return new OperationResult<>(value, next);
    }

    public boolean requiresDialog() {
        return decisions.stream().anyMatch(UserVisibleDecision::requiresDialog);
    }

    public List<UserVisibleDecision> dialogDecisions() {
        return decisions.stream().filter(UserVisibleDecision::requiresDialog).toList();
    }
}
