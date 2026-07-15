package com.marcosmoreiradev.docupodcaststudio.presentation.ribbon;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RibbonStateCoordinatorTest {
    @Test
    void startsExpandedAndClearsLegacyCollapsedPreference() throws Exception {
        Preferences preferences = Preferences.userRoot()
                .node("docupodcaststudio-tests/ribbon-" + UUID.randomUUID());
        try {
            preferences.putBoolean("ribbonCollapsed", true);

            RibbonStateCoordinator coordinator = new RibbonStateCoordinator(preferences);

            assertFalse(coordinator.collapsedProperty().get());
            assertNull(preferences.get("ribbonCollapsed", null));

            coordinator.toggleCollapsed();

            assertTrue(coordinator.collapsedProperty().get());
            assertNull(preferences.get("ribbonCollapsed", null));
            assertFalse(new RibbonStateCoordinator(preferences).collapsedProperty().get());
        } finally {
            preferences.removeNode();
        }
    }
}
