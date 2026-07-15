package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/** Compact reading transport for narrow side rails. */
public final class RailReadingControlBar extends VBox {
    public RailReadingControlBar(
            ObservableValue<String> actionLabel,
            Runnable primaryAction,
            Runnable onPause,
            Runnable onResume,
            Runnable onStop,
            Runnable onPreviousFragment,
            Runnable onNextFragment,
            ObservableValue<Boolean> previousAvailable,
            ObservableValue<Boolean> nextAvailable,
            ObservableValue<Boolean> speedAvailable,
            ObservableValue<? extends Number> playbackRate,
            Runnable onSpeed1x,
            Runnable onSpeed15x,
            Runnable onSpeed175x,
            Runnable refreshAction) {
        super(5);
        getStyleClass().add("document-rail-playback-controls");
        setAlignment(Pos.CENTER);
        setFillWidth(false);
        getChildren().addAll(
                primaryButton(actionLabel, primaryAction),
                iconButton(AppIcon.PAUSE, "Pausar lectura", onPause),
                iconButton(AppIcon.RESUME, "Reanudar lectura", onResume),
                iconButton(AppIcon.STOP, "Detener lectura", onStop),
                speedButton("1x", 1.0, onSpeed1x, speedAvailable, playbackRate),
                speedButton("1.5x", 1.5, onSpeed15x, speedAvailable, playbackRate),
                speedButton("1.75x", 1.75, onSpeed175x, speedAvailable, playbackRate),
                availableIconButton(AppIcon.PREVIOUS_FRAGMENT, "Fragmento anterior", onPreviousFragment, previousAvailable),
                availableIconButton(AppIcon.NEXT_FRAGMENT, "Siguiente fragmento", onNextFragment, nextAvailable),
                iconButton(AppIcon.REFRESH, "Refrescar contenido", refreshAction));
    }

    private Button primaryButton(ObservableValue<String> actionLabel, Runnable action) {
        Button button = iconButton(AppIcon.LISTEN, "Reproducir documento", action);
        Tooltip tooltip = new Tooltip(labelOrDefault(actionLabel == null ? "" : actionLabel.getValue()));
        if (actionLabel != null) {
            tooltip.textProperty().bind(Bindings.createStringBinding(
                    () -> labelOrDefault(actionLabel.getValue()),
                    actionLabel));
        }
        button.setTooltip(tooltip);
        button.accessibleTextProperty().bind(tooltip.textProperty());
        button.getStyleClass().add("document-rail-playback-primary");
        return button;
    }

    private Button availableIconButton(AppIcon icon, String tooltip, Runnable action, ObservableValue<Boolean> available) {
        Button button = iconButton(icon, tooltip, action);
        bindAvailability(button, available);
        return button;
    }

    private Button iconButton(AppIcon icon, String tooltip, Runnable action) {
        Button button = ActionButtonFactory.iconOnly(
                icon,
                tooltip,
                action,
                AppStyles.UI_ACTION_BUTTON,
                AppStyles.UI_ACTION_BUTTON_SECONDARY,
                "document-rail-playback-button");
        return button;
    }

    private Button speedButton(
            String label,
            double rate,
            Runnable action,
            ObservableValue<Boolean> available,
            ObservableValue<? extends Number> playbackRate) {
        Button button = ActionButtonFactory.transport(label, action);
        button.getStyleClass().add("document-rail-playback-button");
        button.getStyleClass().add("document-rail-playback-speed");
        bindAvailability(button, available);
        if (playbackRate != null) {
            playbackRate.addListener((obs, oldValue, newValue) -> markActiveRate(button, rate, newValue));
            markActiveRate(button, rate, playbackRate.getValue());
        }
        return button;
    }

    private void bindAvailability(Button button, ObservableValue<Boolean> available) {
        if (button == null || available == null) {
            return;
        }
        button.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !Boolean.TRUE.equals(available.getValue()), available));
    }

    private void markActiveRate(Button button, double target, Number current) {
        boolean active = current != null && Math.abs(current.doubleValue() - target) < 0.01;
        button.getStyleClass().remove("ui-playback-speed-active");
        if (active) {
            button.getStyleClass().add("ui-playback-speed-active");
        }
    }

    private static String labelOrDefault(String label) {
        String normalized = label == null ? "" : label.strip();
        return normalized.isBlank() ? "Reproducir documento" : normalized;
    }
}
