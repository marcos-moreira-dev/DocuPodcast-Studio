package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreAudioTrackRailSingleClickSourceTest {
    @Test
    void onePrimaryClickSelectsByIdAndDefersNavigation() throws Exception {
        String rail = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAudioTrackRailView.java"));
        String panel = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAudioTrackPanel.java"));

        assertTrue(rail.contains("event.getClickCount() != 1"));
        assertTrue(rail.contains("Platform.runLater"));
        assertTrue(rail.contains("editor.selectTrack(current, true)"));
        assertTrue(rail.contains("left.track().id().equals(right.track().id())"));
        assertFalse(rail.contains("event.getClickCount() == 2"));
        assertTrue(panel.contains("synchronizeTrackEntries"));
        assertTrue(panel.contains("Objects.equals(trackEntries.get(index).track().id(), safe.get(index).track().id())"));
    }
}
