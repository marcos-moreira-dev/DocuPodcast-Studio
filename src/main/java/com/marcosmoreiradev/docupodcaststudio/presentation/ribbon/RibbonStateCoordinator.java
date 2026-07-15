package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Objects;
import java.util.prefs.Preferences;

/** Keeps small ribbon view state for the current desktop session. */
public final class RibbonStateCoordinator {
    private static final String PREF_RIBBON_COLLAPSED = "ribbonCollapsed";

    private final BooleanProperty collapsed = new SimpleBooleanProperty(false);

    public RibbonStateCoordinator(Preferences preferences) {
        Objects.requireNonNull(preferences, "preferences").remove(PREF_RIBBON_COLLAPSED);
    }

    public BooleanProperty collapsedProperty() {
        return collapsed;
    }

    public void toggleCollapsed() {
        collapsed.set(!collapsed.get());
    }
}
