package com.marcosmoreiradev.docupodcaststudio.presentation.ux;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DesktopChromeControlsSourceTest {
    @Test
    void scrollbarsAndTabsUseModernSharedChromeCss() throws Exception {
        String imports = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));
        String chrome = Files.readString(Path.of("src/main/resources/css/components/chrome-controls.css"));

        assertTrue(imports.contains("components/chrome-controls.css"));
        assertTrue(chrome.contains(".scroll-bar:vertical"));
        assertTrue(chrome.contains(".scroll-bar:horizontal"));
        assertTrue(chrome.contains(".tab-pane"));
        assertTrue(chrome.contains(".tab-header-area"));
        assertTrue(chrome.contains(".context-menu"));
        assertTrue(chrome.contains("menu-item .label"));
    }
}
