package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SideDockSelectionVisualPolicyTest {
    @Test
    void keyboardFocusDoesNotImpersonateTheActiveModule() throws IOException {
        String css = Files.readString(Path.of("src", "main", "resources", "css", "compat-legacy.css"),
                StandardCharsets.UTF_8);

        assertFalse(css.contains(".ui-side-dock-rail-button:hover,\n.ui-side-dock-rail-button:focused"));
        assertTrue(css.contains(".ui-side-dock-rail-button:focused"));
        assertTrue(css.contains(".side-dock-rail-button-active:focused"));
    }
}
