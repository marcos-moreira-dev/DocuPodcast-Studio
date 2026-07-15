package com.marcosmoreiradev.docupodcaststudio.presentation.toolbar;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MainToolbarScrollSourceTest {
    @Test
    void toolbarRowsShouldBeScrollableAndUseFullButtonText() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        assertTrue(source.contains("new ScrollPane(row)"));
        assertTrue(source.contains("setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)"));
        assertTrue(source.contains("setMinWidth(Region.USE_PREF_SIZE)"));
        assertTrue(source.contains("setPrefWidth(Math.max"));
        assertFalse(source.contains("Whisper/STT"), "User-facing toolbar should prefer product language over implementation jargon.");
        assertFalse(source.contains("Play selección"), "Toolbar should use Spanish product wording.");
    }
}
