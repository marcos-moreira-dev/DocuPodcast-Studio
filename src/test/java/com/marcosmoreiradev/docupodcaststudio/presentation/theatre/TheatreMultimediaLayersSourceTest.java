package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreMultimediaLayersSourceTest {
    @Test
    void theatreDockExposesPermanentImageAndAudioModesWithVerticalTrackEditor() throws Exception {
        String dock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java"));
        String modes = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreMultimediaLayersPane.java"));
        String audio = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAudioTrackPanel.java"));
        String audioRail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAudioTrackRailView.java"));
        String audioWorkspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAudioTrackWorkspace.java"));
        String split = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleModuleSplitPane.java"));

        assertTrue(dock.contains("Capas multimedia"));
        assertTrue(modes.contains("AppIcon.IMAGE"));
        assertTrue(modes.contains("AppIcon.AUDIO"));
        assertTrue(audio.contains("Guardar pista"));
        assertTrue(audio.contains("Hasta el final"));
        assertTrue(audio.contains("new ScrollPane"));
        assertTrue(audio.contains("Establecer inicio"));
        assertTrue(audio.contains("Establecer fin"));
        assertTrue(audio.contains("Fade suave"));
        assertTrue(audio.contains("Preparando audio..."));
        assertTrue(audio.contains("prepareAudio(projectFile, chosen)"));
        assertTrue(audio.contains("setReady(false"));
        assertTrue(audioRail.contains("Pistas asignadas"));
        assertTrue(audioWorkspace.contains("CollapsibleModuleSplitPane"));
        assertTrue(modes.contains("expandImage.run()"));
        assertTrue(modes.contains("expandAudio.run()"));
        assertTrue(split.contains("!showPrimaryCollapsedStrip"));
    }
}
