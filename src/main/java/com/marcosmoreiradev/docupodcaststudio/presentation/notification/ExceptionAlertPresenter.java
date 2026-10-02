package com.marcosmoreiradev.docupodcaststudio.presentation.notification;

import com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialog;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Presents product notifications and technical errors as consistent JavaFX dialogs. */
public final class ExceptionAlertPresenter {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExceptionAlertPresenter.class);

    public void show(UserNotification notification, Window owner) {
        if (notification == null) return;
        try {
            // Presentation is deferred to the next JavaFX pulse. This avoids
            // mutating dialog/layout state from a worker or an active pulse.
            Platform.runLater(() -> showSafely(notification, owner));
        } catch (RuntimeException presentationFailure) {
            LOGGER.error("Could not schedule the JavaFX notification: {}",
                    notification.headline(), presentationFailure);
        }
    }

    public void showFailure(String headline, Throwable error, Window owner) {
        LOGGER.error("Application operation failed: {}", headline, error);
        show(UserNotification.failure(headline, error), owner);
    }

    public void showDecision(UserVisibleDecision decision, Window owner) {
        if (decision != null && decision.requiresDialog()) {
            show(UserNotification.fromDecision(decision), owner);
        }
    }

    public void showDialogDecisions(List<UserVisibleDecision> decisions, Window owner) {
        if (decisions == null || decisions.isEmpty()) {
            return;
        }
        decisions.stream()
                .filter(UserVisibleDecision::requiresDialog)
                .forEach(decision -> show(UserNotification.fromDecision(decision), owner));
    }

    private static Alert.AlertType alertType(UserNotificationLevel level) {
        return switch (level) {
            case SUCCESS, INFORMATION -> Alert.AlertType.INFORMATION;
            case WARNING -> Alert.AlertType.WARNING;
            case ERROR -> Alert.AlertType.ERROR;
        };
    }

    private static void showSafely(UserNotification notification, Window owner) {
        try {
            Alert alert = StudioMessageDialog.create(
                    owner,
                    alertType(notification.level()),
                    notification.title(),
                    notification.headline(),
                    notification.message(),
                    notification.technicalDetail());
            alert.show();
        } catch (RuntimeException presentationFailure) {
            LOGGER.error("Could not present the JavaFX notification: {}",
                    notification.headline(), presentationFailure);
        }
    }
}
