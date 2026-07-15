package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF3B guardrail: the Voices workspace must offer operational paths, not only explanatory panels. */
final class VoiceLibraryOperationalUxPf3BSourceTest {
    @Test
    void voiceWorkspaceLinksOperationalEngineSetupAndStatusActions() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String actions = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfilePresentationPolicy.java"));
        String voices = view + actions + policy;
        assertTrue(view.contains("Configurar motor"));
        assertTrue(view.contains("new SettingsDialog().showVoiceEngines"));
        assertFalse(view.contains("Ver estado de voces"));
        assertTrue(voices.contains("Reproducir muestra"));
        assertTrue(voices.contains("Exportar muestra"));
        assertTrue(voices.contains("Eliminar muestra"));
        assertTrue(voices.contains("Usa voz base del motor"));
    }
}
