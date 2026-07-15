package com.marcosmoreiradev.docupodcaststudio.presentation.guide;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuideDialogRenderedSourceTest {
    @Test
    void guideRendersTopicsAsNodesInsteadOfRawMarkdownEditor() throws Exception {
        String guide = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/guide/GuideDialog.java"));
        String css = Files.readString(Path.of("src/main/resources/css/guide.css"));
        String topics = Files.readString(Path.of("src/main/resources/help/topics/getting-started.md"))
                + "\n" + Files.readString(Path.of("src/main/resources/help/topics/audio-generation.md"))
                + "\n" + Files.readString(Path.of("src/main/resources/help/topics/storyboard-live.md"));

        assertFalse(guide.contains("TextArea"), "La guía visible no debe usar TextArea como visor de Markdown crudo.");
        assertTrue(guide.contains("guide-body-rendered"));
        assertTrue(guide.contains("renderMarkup"));
        assertTrue(css.contains("guide-topic-title-rendered"));
        assertFalse(topics.contains("Whisper"));
        assertFalse(topics.contains("Audio a texto"));
        assertTrue(topics.toLowerCase(java.util.Locale.ROOT).contains("bloque visual"));
        assertTrue(topics.contains("5 segundos"));
    }
}
