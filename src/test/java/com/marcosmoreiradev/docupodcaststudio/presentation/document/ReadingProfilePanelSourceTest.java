package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadingProfilePanelSourceTest {
    @Test
    void readingProfilePanelSupportsEditPreviewPersistAndApply() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentReadingProfilePanel.java"));

        assertTrue(source.contains("Guardar perfil en el proyecto"));
        assertTrue(source.contains("Previsualizar impacto"));
        assertTrue(source.contains("Guardar y aplicar al documento"));
        assertTrue(source.contains("ImageNarrationPolicy"));
        assertTrue(source.contains("TableNarrationPolicy"));
        assertTrue(source.contains("ReadingProfilePreview"));
    }
}
