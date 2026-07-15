package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

/** Icon-only tab for the contextual left sidebar. */
public final class SidebarIconTab extends ToggleButton {
    public SidebarIconTab(String icon, String accessibleText, Runnable action) {
        super(icon == null || icon.isBlank() ? "•" : icon);
        getStyleClass().add(AppStyles.UI_SIDEBAR_ICON_TAB);
        setFocusTraversable(false);
        setAccessibleText(accessibleText == null || accessibleText.isBlank() ? "Módulo" : accessibleText);
        Tooltip.install(this, new Tooltip(getAccessibleText()));
        if (action != null) {
            setOnAction(event -> action.run());
        }
    }
}
