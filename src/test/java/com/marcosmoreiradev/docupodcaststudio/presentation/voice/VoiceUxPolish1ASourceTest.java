package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOICE-UX-POLISH1A: Voice workspace cleanup keeps only operational voice-library controls. */
final class VoiceUxPolish1ASourceTest {
    @Test
    void voicesHomeRemovesDeadStatusAndDashboardSummary() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));

        assertTrue(view.contains("Consulta qué voces puede usar Documento"));
        assertFalse(view.contains("Ver estado de voces"));
        assertFalse(view.contains("Resumen de operación"));
        assertFalse(view.contains("homeOperationalSummary"));
        assertTrue(view.contains("Biblioteca operativa para lectura simple y voces avanzadas"));
    }

    @Test
    void voiceToneCombosUseSimpleEmotionNamesOnly() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceToneLabelPolicy.java"));
        String documentPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));

        assertTrue(view.contains("VoiceToneLabelPolicy.comboLabel(prompt.tone())"));
        assertTrue(policy.contains("tone.displayName()"));
        assertFalse(view.contains("prompt.category().displayName()) + \" · \""));
        assertFalse(view.contains("Tonos recomendados ·"));
        assertFalse(view.contains("Catálogo teatral extendido ·"));
        assertTrue(documentPanel.contains("VoiceToneLabelPolicy.comboLabel(tone)"));
        assertTrue(documentPanel.contains("sampleSet.registeredTones()"));
    }

    @Test
    void voiceRowsShowOnlyRegisteredToneTagsAndOperationalStatus() throws Exception {
        String row = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceListItemView.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfilePresentationPolicy.java"));
        String css = Files.readString(Path.of("src/main/resources/css/voice-library.css"));
        String source = row + policy;

        assertTrue(source.contains("Voz simple"));
        assertTrue(source.contains("Voz avanzada"));
        assertTrue(row.contains("registeredToneTags"));
        assertTrue(row.contains("set.registeredTones()"));
        assertFalse(source.contains("Tonos registrados:"));
        assertFalse(source.contains("Falta neutral"));
        assertTrue(css.contains(".voice-list-tone-tag"));
    }

    @Test
    void engineAndGeneratedTestStatusAreReadableNonButtonText() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String controls = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java"));
        String css = Files.readString(Path.of("src/main/resources/css/voice-library.css"));

        assertTrue((view + controls).contains("voice-engine-status"));
        assertTrue(css.contains(".voice-engine-status"));
        assertTrue(css.contains("-fx-text-fill: -docu-text-muted;"));
        assertFalse(css.contains(".voice-engine-status {\n    -fx-text-fill: white"));
        assertTrue(css.contains(".voice-generated-test-status"));
        assertTrue(css.contains("-fx-background-color: transparent;"));
    }
}
