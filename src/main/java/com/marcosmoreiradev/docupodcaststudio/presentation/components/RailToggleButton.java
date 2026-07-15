package com.marcosmoreiradev.docupodcaststudio.presentation.components;

/** Explicit show/hide control for the right media rail. */
public final class RailToggleButton extends SidePanelToggleButton {
    public RailToggleButton(AppIcon icon, String tooltip, Runnable action) {
        super(icon, tooltip, action);
        getStyleClass().add(AppStyles.UI_RAIL_TOGGLE_BUTTON);
    }

    public RailToggleButton(String text, String tooltip, Runnable action) {
        this(text == null || text.isBlank() ? AppIcon.EXPAND : AppIcon.COLLAPSE,
                tooltip == null || tooltip.isBlank() ? "Mostrar u ocultar miniaturas" : tooltip,
                action);
    }
}
