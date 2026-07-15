package com.marcosmoreiradev.docupodcaststudio.presentation.notification;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.DecisionSeverity;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.application.errors.UserFacingApplicationException;

/**
 * Product notification emitted by presentation flows before a JavaFX presenter decides
 * how it should be shown to the user.
 */
public record UserNotification(
        UserNotificationLevel level,
        String title,
        String headline,
        String message,
        String technicalDetail
) {
    public UserNotification {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
        title = normalize(title, "DocuPodcast Studio");
        headline = normalize(headline, "Operacion de DocuPodcast Studio");
        message = normalize(message, "No hay detalle disponible.");
        technicalDetail = technicalDetail == null ? "" : technicalDetail.strip();
    }

    public static UserNotification success(String headline, String message) {
        return new UserNotification(UserNotificationLevel.SUCCESS, "DocuPodcast Studio", headline, message, "");
    }

    public static UserNotification information(String headline, String message) {
        return new UserNotification(UserNotificationLevel.INFORMATION, "DocuPodcast Studio", headline, message, "");
    }

    public static UserNotification warning(String headline, String message) {
        return new UserNotification(UserNotificationLevel.WARNING, "DocuPodcast Studio", headline, message, "");
    }

    public static UserNotification fromDecision(UserVisibleDecision decision) {
        if (decision == null) {
            return information("Decisión de DocuPodcast Studio", "No hay detalle disponible.");
        }
        return new UserNotification(
                levelFromDecision(decision.severity()),
                decision.title(),
                decision.headline(),
                decision.message(),
                decision.technicalDetail()
        );
    }

    public static UserNotification failure(String headline, Throwable error) {
        if (error instanceof UserFacingApplicationException userFacing) {
            return new UserNotification(
                    levelFromDecision(userFacing.severity()),
                    "DocuPodcast Studio",
                    userFacing.userHeadline().isBlank() ? headline : userFacing.userHeadline(),
                    userFacing.userMessage(),
                    userFacing.technicalDetail()
            );
        }
        return new UserNotification(
                UserNotificationLevel.ERROR,
                "DocuPodcast Studio",
                headline,
                productMessage(error),
                technicalMessage(error)
        );
    }

    private static UserNotificationLevel levelFromDecision(DecisionSeverity severity) {
        return switch (severity == null ? DecisionSeverity.INFORMATION : severity) {
            case INFORMATION -> UserNotificationLevel.INFORMATION;
            case WARNING -> UserNotificationLevel.WARNING;
            case ERROR -> UserNotificationLevel.ERROR;
        };
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.strip();
    }

    private static String productMessage(Throwable error) {
        if (error == null) {
            return "No se pudo completar la operacion solicitada.";
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.strip();
    }

    private static String technicalMessage(Throwable error) {
        if (error == null) {
            return "";
        }
        String message = error.getClass().getName() + ": " + productMessage(error);
        Throwable cause = error.getCause();
        if (cause != null) {
            message += System.lineSeparator() + "Causa: " + cause.getClass().getName() + ": " + productMessage(cause);
        }
        return message;
    }
}
