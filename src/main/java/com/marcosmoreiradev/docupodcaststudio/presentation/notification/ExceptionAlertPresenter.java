package com.marcosmoreiradev.docupodcaststudio.presentation.notification;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Window;

import java.util.List;

/** Presents product notifications and technical errors as consistent JavaFX dialogs. */
public final class ExceptionAlertPresenter {
    public void show(UserNotification notification, Window owner) {
        Alert alert = new Alert(alertType(notification.level()));
        alert.setTitle(notification.title());
        alert.setHeaderText(notification.headline());
        alert.setContentText(notification.message());
        if (!notification.technicalDetail().isBlank()) {
            TextArea details = new TextArea(notification.technicalDetail());
            details.setEditable(false);
            details.setWrapText(true);
            details.setPrefColumnCount(80);
            details.setPrefRowCount(8);
            alert.getDialogPane().setExpandableContent(details);
        }
        DialogStyler.apply(alert, owner);
        alert.showAndWait();
    }

    public void showFailure(String headline, Throwable error, Window owner) {
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
}
