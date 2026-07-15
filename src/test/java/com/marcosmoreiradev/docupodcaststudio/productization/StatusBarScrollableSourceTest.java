package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** STATUS-SCROLL1: the long bottom status message must remain readable. */
final class StatusBarScrollableSourceTest {
    @Test
    void statusBarWrapsTheMessageInAHorizontalScrollPane() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java"));
        assertTrue(source.contains("new ScrollPane(label)"));
        assertTrue(source.contains("statusScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)"));
        assertTrue(source.contains("statusScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)"));
        assertTrue(source.contains("statusScroller.setPannable(true)"));
        assertTrue(source.contains("addEventFilter(javafx.scene.input.ScrollEvent.SCROLL"));
        assertTrue(source.contains("refreshScrollableStatusWidth(label, statusScroller)"));
    }

    @Test
    void statusBarCssKeepsTheHorizontalScrollerCompact() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/css/statusbar.css"));
        assertTrue(css.contains(".status-bar-scroll"));
        assertTrue(css.contains(".status-bar-scroll-hidden .scroll-bar:horizontal"));
    }
}
