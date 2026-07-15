package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UX-RIBBON3 keeps the right visual panel owned by the document, not by a global ribbon button. */
final class RibbonAndRailCleanupUxRibbon3SourceTest {
    @Test
    void ribbonDoesNotExposeRightRailAsGlobalViewAction() throws Exception {
        String ribbon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java"));
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        assertFalse(ribbon.contains("AppCommandId.TOGGLE_RIGHT_RAIL"));
        assertFalse(ribbon.contains("group(\"Panel visual\""));
        assertTrue(registry.contains("AppCommandSurface.RIGHT_RAIL"));
        assertTrue(registry.contains("Mostrar u ocultar panel visual"));
    }

    @Test
    void collapsibleRailUsesIconOnlyToggleWithoutDuplicateGlyphText() throws Exception {
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java"));
        String toggle = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SidePanelToggleButton.java"));
        assertTrue(rail.contains("toggle.setIcon(AppIcon.COLLAPSE"));
        assertTrue(rail.contains("toggle.setIcon(AppIcon.EXPAND"));
        assertTrue(toggle.contains("setText(\"\")"));
        assertTrue(toggle.contains("IconView.rail(safeIcon)"));
        assertFalse(rail.contains("toggle.setText(\"<\")"));
        assertFalse(rail.contains("toggle.setText(\">\")"));
    }
}
