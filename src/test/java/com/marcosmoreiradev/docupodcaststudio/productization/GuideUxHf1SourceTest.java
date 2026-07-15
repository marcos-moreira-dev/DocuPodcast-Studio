package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GUIDE-UX-HF1 keeps the integrated guide practical and task-oriented. */
final class GuideUxHf1SourceTest {
    @Test
    void guideTopicsAreOperationalAndNotDecorative() throws Exception {
        String start = read("src/main/resources/help/topics/getting-started.md");
        String document = read("src/main/resources/help/topics/reading-profile.md");
        String voices = read("src/main/resources/help/topics/voices-characters-styles.md");
        String audio = read("src/main/resources/help/topics/audio-generation.md");
        String export = read("src/main/resources/help/topics/exporting.md");
        String trouble = read("src/main/resources/help/topics/troubleshooting.md");

        assertTrue(start.contains("Abre tu Word") && start.contains("Guarda el proyecto"));
        assertTrue(document.contains("Seleccionar una oración") && document.contains("Asignar imagen"));
        assertTrue(voices.contains("Graba o importa la muestra **Neutral**"));
        assertTrue(audio.contains("Voz local simple") && audio.contains("Voz IA avanzada"));
        assertTrue(export.contains("Antes de exportar") && export.contains("MP4 final"));
        assertTrue(trouble.contains("Seleccioné GPU pero usa CPU"));
        assertFalse(start.toLowerCase().contains("dashboard"));
    }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }
}
