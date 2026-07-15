package com.marcosmoreiradev.docupodcaststudio.application.decisions;

import java.util.Objects;

/**
 * A decision that the application took while executing a user action and that may need
 * to be shown to the user.
 *
 * <p>Use this for defensive fallbacks that change the user's explicit intention but still
 * allow the operation to continue. Failures that stop the operation should use the typed
 * exceptions under {@code application.errors}.</p>
 */
public record UserVisibleDecision(
        DecisionSeverity severity,
        String title,
        String headline,
        String message,
        String technicalDetail,
        boolean requiresDialog
) {
    public UserVisibleDecision {
        severity = Objects.requireNonNullElse(severity, DecisionSeverity.INFORMATION);
        title = normalize(title, "DocuPodcast Studio");
        headline = normalize(headline, "Decisión de DocuPodcast Studio");
        message = normalize(message, "DocuPodcast tomó una decisión operativa para completar la acción.");
        technicalDetail = technicalDetail == null ? "" : technicalDetail.strip();
    }

    public static UserVisibleDecision information(String headline, String message) {
        return new UserVisibleDecision(DecisionSeverity.INFORMATION, "DocuPodcast Studio", headline, message, "", false);
    }

    public static UserVisibleDecision informationDialog(String headline, String message) {
        return new UserVisibleDecision(DecisionSeverity.INFORMATION, "DocuPodcast Studio", headline, message, "", true);
    }

    public static UserVisibleDecision warning(String headline, String message) {
        return new UserVisibleDecision(DecisionSeverity.WARNING, "DocuPodcast Studio", headline, message, "", true);
    }

    public static UserVisibleDecision error(String headline, String message, String technicalDetail) {
        return new UserVisibleDecision(DecisionSeverity.ERROR, "DocuPodcast Studio", headline, message, technicalDetail, true);
    }

    public static UserVisibleDecision defensiveFallback(String headline, String message, String technicalDetail) {
        return new UserVisibleDecision(DecisionSeverity.WARNING, "DocuPodcast Studio", headline, message, technicalDetail, true);
    }

    public boolean blocksOperation() {
        return severity == DecisionSeverity.ERROR;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
