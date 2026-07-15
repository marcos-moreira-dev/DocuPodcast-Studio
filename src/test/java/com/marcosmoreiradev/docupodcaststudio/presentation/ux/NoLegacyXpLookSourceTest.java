package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NoLegacyXpLookSourceTest {
    @Test
    void coreChromeAvoidsLegacyGradientButtonLook() throws Exception {
        String toolbar = Files.readString(Path.of("src/main/resources/css/toolbar.css"));
        String shell = Files.readString(Path.of("src/main/resources/css/shell.css"));
        String status = Files.readString(Path.of("src/main/resources/css/statusbar.css"));
        String document = Files.readString(Path.of("src/main/resources/css/document-reader.css"))
                + Files.readString(Path.of("src/main/resources/css/document/document-page.css"));

        assertFalse(toolbar.contains("linear-gradient"));
        assertFalse(shell.contains("linear-gradient"));
        assertFalse(status.contains("linear-gradient"));
        assertTrue(toolbar.contains("flat, modern toolbar"));
        assertTrue(document.contains("modern page"));
        assertTrue(document.contains("-fx-font-size: 18px"));
        assertTrue(document.contains("-fx-line-spacing: 7px"));
    }
}
