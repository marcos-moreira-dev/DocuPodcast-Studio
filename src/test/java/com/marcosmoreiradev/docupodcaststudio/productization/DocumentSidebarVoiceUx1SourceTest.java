package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOCUMENT-SIDEBAR-VOICE-UX1: Documento separates generated voice from computer audio and keeps tone combos simple. */
final class DocumentSidebarVoiceUx1SourceTest {
    @Test
    void documentSidebarSeparatesGeneratedVoiceFromComputerAudio() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");

        assertTrue(panel.contains("Voz generada"));
        assertTrue(panel.contains("Audio del computador"));
        assertTrue(panel.contains("Este camino no usa voz generada"));
        assertTrue(panel.contains("El documento fuente no cambia. La voz, el audio y la regla de ':'"));
        assertTrue(panel.contains("Al cambiar la voz del documento se regeneran los fragmentos de audio"));
        assertTrue(panel.contains("las voces específicas de fragmento se respetan"));
        assertFalse(panel.contains("Quitar voz/audio asignado"));
    }

    @Test
    void documentToneComboUsesOnlySimpleRegisteredToneNames() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");

        assertTrue(panel.contains("VoiceToneLabelPolicy.comboLabel"));
        assertTrue(panel.contains("registeredDocumentTones"));
        assertTrue(panel.contains("sampleSet.registeredTones()"));
        assertTrue(panel.contains("label(\"Tono\")"));
        assertFalse(panel.contains("Tonos recomendados ·"));
        assertFalse(panel.contains("Catálogo teatral extendido ·"));
        assertFalse(panel.contains("Tono de referencia"));
    }

    @Test
    void documentSidebarKeepsAdvancedVoiceAndComputerAudioConceptuallySeparate() throws Exception {
        String docs = read("docs/productizacion/DOCUMENT_SIDEBAR_VOICE_UX1_VOZ_IA_AUDIO_LOCAL.md");
        assertTrue(docs.contains("Voz generada"));
        assertTrue(docs.contains("Audio del computador"));
        assertTrue(docs.contains("no son clips fijos"));
        assertTrue(docs.contains("solo muestra tonos registrados"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
