package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T121-V08: final product-oriented voice library layout before component extraction/CSS hardening. */
final class VoiceLibraryFinalRedesignT121V08SourceTest {
    @Test
    void voiceLibraryUsesProductHeroEngineCardsAndToneSampleMatrix() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String editor = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java"));
        String css = Files.readString(Path.of("src/main/resources/css/voice-library.css"));

        assertTrue(view.contains("voiceLibraryHero"));
        assertFalse(view.contains("homeOperationalSummary"));
        assertTrue(view.contains("Prueba rápida del motor seleccionado"));
        assertTrue(view.contains("voice-engine-test-card"));
        assertFalse(view.contains("detailStack(engineModeOverviewSection(report), card)"));
        assertTrue(view.contains("referenceSampleMatrix"));
        assertTrue(view.contains("referenceSampleRow"));
        assertTrue(view.contains("voice-sample-grid"));
        assertTrue(editor.contains("Referencia sonora por emoción"));
        assertTrue(css.contains("voice-library-hero"));
        assertTrue(css.contains("voice-engine-test-card"));
        assertTrue(css.contains("voice-sample-row"));
        assertTrue(css.contains("voice-tone-badge-required"));
    }

    @Test
    void voiceLibraryKeepsTransversalActionsAndAvoidsTechnicalEngineNames() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String engineControls = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java"));
        String overview = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java"));
        String visible = view + "\n" + engineControls + "\n" + overview;

        assertTrue(view.contains("ActionButtonFactory.primary"));
        assertTrue(view.contains("ActionButtonFactory.secondary"));
        assertTrue(visible.contains("Voz IA avanzada"));
        assertTrue(visible.contains("Voz local simple"));
        assertTrue(engineControls.contains("Modo de prueba"));
        assertFalse(view.contains("setStyle("));
        assertFalse(view.contains("\"Coqui\""));
        assertFalse(view.contains("\"XTTS\""));
        assertFalse(view.contains("\"Piper\""));
    }
}
