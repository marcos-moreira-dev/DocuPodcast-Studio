package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-TTS5A: Documento only exposes voices and tones that are actually usable by the active engine. */
final class VoiceTts5ADocumentRealVoiceToneSourceTest {
    @Test
    void documentAdvancedVoiceRequiresNeutralReferenceAndRegisteredTones() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");

        assertTrue(panel.contains("usableInDocumentForEngine"));
        assertTrue(panel.contains("profile.coquiXttsMode()"));
        assertTrue(panel.contains("referenceSampleSetByVoiceId(voice.id()).filter(sampleSet -> sampleSet.hasNeutral()).isPresent()"));
        assertTrue(panel.contains("registeredDocumentTones"));
        assertTrue(panel.contains("sampleSet.registeredTones()"));
        assertTrue(panel.contains("neutralFirst"));
        assertTrue(panel.contains("Registra una voz con muestra Neutral"));
        assertFalse(panel.contains("VoiceReferenceTone.values()"),
                "Documento no debe mostrar el catálogo global de tonos; solo tonos registrados para la voz elegida.");
        assertFalse(panel.contains("java.util.Arrays.asList(VoiceReferenceTone"));
    }

    @Test
    void documentKeepsSimpleLocalVoiceWithoutExpressiveToneControls() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");

        assertTrue(panel.contains("La Voz local simple usa la voz local disponible"));
        assertTrue(panel.contains("registeredDocumentTones"));
        assertTrue(panel.contains("!profile.supportsAdvancedExpressiveControls()"));
        assertTrue(panel.contains("El campo Tono solo mostrará emociones que esa voz ya tenga grabadas o importadas"));
    }

    @Test
    void documentationRegistersVoiceTts5A() throws Exception {
        String current = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String roadmap = read("DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md");
        String validation = read("VALIDATION.md");
        String doc = read("docs/productizacion/VOZ_TTS5A_DOCUMENTO_VOCES_TONOS_REALES.md");

        assertTrue(current.contains("VOZ-TTS5A"));
        assertTrue(roadmap.contains("VOZ-TTS5A"));
        assertTrue(validation.contains("VoiceTts5ADocumentRealVoiceToneSourceTest"));
        assertTrue(doc.contains("muestra Neutral"));
        assertTrue(doc.contains("tonos registrados"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
