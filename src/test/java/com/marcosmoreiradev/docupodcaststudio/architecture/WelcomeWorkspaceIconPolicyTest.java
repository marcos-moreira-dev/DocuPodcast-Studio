package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class WelcomeWorkspaceIconPolicyTest {
    @Test
    void keepsLargeProductIconsWithoutDuplicatingCompactActionGlyphs() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java",
                "com", "marcosmoreiradev", "docupodcaststudio", "presentation",
                "welcome", "WelcomeWorkspaceView.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("IconView.welcome"));
        assertTrue(source.contains("RibbonIconCatalog.iconFor"));
        assertTrue(source.contains("button.setGraphic(null)"));
        assertTrue(source.contains("LucideIconView.of(\"folder-open\")"));
        assertTrue(source.contains("ScrollPane.ScrollBarPolicy.AS_NEEDED"));
        assertTrue(source.contains("scroll.setFitToHeight(false)"));
        assertTrue(source.contains("StudioViewportControls.scrollPane(commands)"));
        assertTrue(source.contains("getChildren().add(stage())"));
    }
}
