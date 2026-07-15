package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Compact reusable status strip for operational surfaces.
 *
 * <p>This component is intentionally small: it communicates the current state or next step for an
 * action area without becoming decorative filler.</p>
 */
public final class OperationalStatusStrip extends HBox {
    private final Label messageLabel;

    public OperationalStatusStrip(String message) {
        getStyleClass().add("ui-operational-status-strip");
        messageLabel = new Label(message == null ? "" : message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("ui-operational-status-message");
        getChildren().add(messageLabel);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);
    }

    public Label messageLabel() {
        return messageLabel;
    }

    public void setMessage(String message) {
        messageLabel.setText(message == null ? "" : message);
    }
}
