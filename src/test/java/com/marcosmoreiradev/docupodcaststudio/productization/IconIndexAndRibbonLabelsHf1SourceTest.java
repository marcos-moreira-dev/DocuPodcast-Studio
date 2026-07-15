package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class IconIndexAndRibbonLabelsHf1SourceTest {
    @Test
    void indexIconAndRibbonLabelAreOperationalAndReadable() throws Exception {
        String registry = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java"));
        String button = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonButton.java"));
        String appIcon = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppIcon.java"));

        assertTrue(Files.isRegularFile(Path.of("src/main/resources/icons/ui/tree-index.png")));
        assertTrue(appIcon.contains("INDEX_TREE(\"tree-index.png\""));
        assertTrue(registry.contains("Reproducir desde aquí"));
        assertTrue(button.contains("setMinWidth(216)"));
        assertTrue(button.contains("setPrefWidth(232)"));
        assertTrue(button.contains("textLabel.setMaxWidth(236)"));
        assertTrue(button.contains("textLabel.setWrapText(true)"));
    }
}
