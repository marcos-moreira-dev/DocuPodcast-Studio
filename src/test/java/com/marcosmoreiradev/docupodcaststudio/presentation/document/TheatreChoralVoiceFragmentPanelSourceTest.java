package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreChoralVoiceFragmentPanelSourceTest {
    @Test
    void exposesTheatreOnlyChoralControlsInFragmentPanelAndNotAudioTrackPanel() throws Exception {
        String fragment = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java"));
        String track = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreAudioTrackPanel.java"));

        assertTrue(fragment.contains("ProjectMode.THEATRE_PRODUCTION"));
        assertTrue(fragment.contains("Todos los personajes (sin narrador)"));
        assertTrue(fragment.contains("Renderizar voz multipersona"));
        assertTrue(fragment.contains("Restaurar voz simple"));
        assertTrue(fragment.contains("ScrollPane scroll"));
        assertFalse(track.contains("Guardar voces simultaneas"));
        assertFalse(track.contains("Importar mezcla multivoz"));
    }
}
