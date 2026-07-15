package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Shared icon-only toggle for document side panels. */
public class SidePanelToggleButton extends Button {
    public SidePanelToggleButton(AppIcon icon, String tooltip, Runnable action) {
        super("");
        getStyleClass().add(AppStyles.UI_SIDE_PANEL_TOGGLE_BUTTON);
        setFocusTraversable(false);
        setIcon(icon, tooltip);
        if (action != null) {
            setOnAction(event -> action.run());
        }
    }

    public final void setIcon(AppIcon icon, String tooltipText) {
        AppIcon safeIcon = icon == null ? AppIcon.COLLAPSE : icon;
        setText("");
        setGraphic(IconView.rail(safeIcon));
        String safeTooltip = tooltipText == null || tooltipText.isBlank()
                ? safeIcon.accessibleText()
                : tooltipText;
        setAccessibleText(safeTooltip);
        Tooltip tooltip = new Tooltip(safeTooltip);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        setTooltip(tooltip);
    }
}
