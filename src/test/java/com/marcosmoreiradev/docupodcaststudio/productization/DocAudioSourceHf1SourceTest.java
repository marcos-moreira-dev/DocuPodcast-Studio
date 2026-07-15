package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-AUDIO-SOURCE-HF1 makes Documento show usable audio origins, not the last configured engine blindly. */
final class DocAudioSourceHf1SourceTest {
    @Test
    void documentAudioOriginListsOnlyOperativeSourcesWithReason() throws Exception {
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertTrue(panel.contains("documentAudioSourceAvailability()"));
        assertTrue(panel.contains("filter(AudioEngineAvailability::usableInDocument)"));
        assertTrue(panel.contains("Voz IA avanzada no aparece aquí si no pasó prueba WAV"));
        assertTrue(panel.contains("sourceStatus"));
        assertTrue(panel.contains("Audio del computador: elige un archivo local"));
        assertTrue(workspace.contains("Origen de audio usable para la selección"));
    }
}
