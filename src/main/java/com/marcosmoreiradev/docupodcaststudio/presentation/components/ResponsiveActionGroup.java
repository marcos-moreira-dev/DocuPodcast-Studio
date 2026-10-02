package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

import java.util.Arrays;
import java.util.List;

/**
 * Keeps horizontal action rows readable when their workspace becomes narrow.
 *
 * <p>Buttons retain their semantic graphic, tooltip and accessible name. Below the supplied
 * width they render as 40 px icon buttons instead of wrapping labels one character per line.</p>
 */
public final class ResponsiveActionGroup {
    private static final double COMPACT_BUTTON_SIZE = 40.0;

    private ResponsiveActionGroup() {
    }

    public static void install(Region container, double compactBelow, Button... buttons) {
        if (container == null || buttons == null || buttons.length == 0) return;
        double threshold = Math.max(COMPACT_BUTTON_SIZE * buttons.length, compactBelow);
        List<ButtonPresentation> presentations = Arrays.stream(buttons)
                .filter(java.util.Objects::nonNull)
                .map(button -> prepare(button, button.getText()))
                .toList();
        Runnable update = () -> apply(presentations, shouldCompact(container.getWidth(), threshold));
        container.widthProperty().addListener((observable, oldWidth, newWidth) -> update.run());
        Platform.runLater(update);
    }

    static boolean shouldCompact(double width, double threshold) {
        return width > 0 && width < threshold;
    }

    static void apply(Button button, String fullLabel, boolean compact) {
        if (button == null) return;
        String label = fullLabel == null ? "" : fullLabel.strip();
        button.setAccessibleText(label);
        if (button.getTooltip() == null && !label.isBlank()) {
            Tooltip tooltip = new Tooltip(label);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(360);
            button.setTooltip(tooltip);
        }
        button.setWrapText(false);
        button.setText(compact ? "" : label);
        button.setContentDisplay(compact ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
        button.setMinWidth(compact ? COMPACT_BUTTON_SIZE : 0);
        button.setPrefWidth(compact ? COMPACT_BUTTON_SIZE : Region.USE_COMPUTED_SIZE);
        button.setMaxWidth(compact ? COMPACT_BUTTON_SIZE : Double.MAX_VALUE);
        if (compact) {
            if (!button.getStyleClass().contains("ui-responsive-action-compact")) {
                button.getStyleClass().add("ui-responsive-action-compact");
            }
        } else {
            button.getStyleClass().remove("ui-responsive-action-compact");
        }
    }

    private static ButtonPresentation prepare(Button button, String fullLabel) {
        String label = fullLabel == null ? "" : fullLabel.strip();
        if (button.getGraphic() == null) SemanticActionIcons.decorate(button, label);
        apply(button, label, false);
        return new ButtonPresentation(button, label);
    }

    private static void apply(List<ButtonPresentation> presentations, boolean compact) {
        presentations.forEach(item -> apply(item.button(), item.fullLabel(), compact));
    }

    private record ButtonPresentation(Button button, String fullLabel) {
    }
}
