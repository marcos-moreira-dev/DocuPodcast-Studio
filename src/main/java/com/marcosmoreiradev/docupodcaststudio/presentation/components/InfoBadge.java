package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.scene.control.Label;

/** Reusable chip/badge for status labels. */
public final class InfoBadge extends Label {
    public InfoBadge(String text, String stateClass) {
        super(text);
        getStyleClass().add(AppStyles.UI_INFO_BADGE);
        if (stateClass != null && !stateClass.isBlank()) {
            getStyleClass().add(stateClass);
        }
    }
}
