package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CollapsibleModuleSplitPaneSourceTest {
    @Test
    void moduleSplitCanFoldPrimaryRegionAndRestoreIt() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleModuleSplitPane.java"));
        String theatreDock = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));
        String css = Files.readString(Path.of("src/main/resources/css/compat-legacy.css"));

        assertTrue(source.contains("BooleanProperty primaryVisible"));
        assertTrue(source.contains("BooleanProperty secondaryVisible"));
        assertTrue(source.contains("secondaryVisibleProperty()"));
        assertTrue(source.contains("new SidePanelToggleButton("));
        assertTrue(source.contains("AppIcon.COLLAPSE"));
        assertTrue(source.contains("AppIcon.EXPAND"));
        assertTrue(source.contains("Ocultar \" + primaryLabel"));
        assertTrue(source.contains("Mostrar \" + labelText"));
        assertTrue(source.contains("split.getItems().setAll(primarySlot, secondary)"));
        assertTrue(source.contains("collapsedContent.getChildren().setAll(strip, secondary)"));
        assertTrue(theatreDock.contains("\"Acciones visuales\""));
        assertTrue(theatreDock.contains("COMPACT_WIDTH"));
        assertTrue(source.contains("showPrimaryCollapsedStrip"));
        assertTrue(source.contains("public void showPrimary()"));
        assertTrue(theatreDock.contains("split, split::showPrimary"));
        assertTrue(css.contains(".ui-collapsible-module-split-strip"));
        assertTrue(css.contains("-fx-border-color: transparent"));
    }
}
