package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentAudioEngineSyncHf9SourceTest {
    @Test
    void audioSourceLabelFollowsActiveEngineInsteadOfHardcodingAdvancedVoice() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        assertTrue(source.contains("voiceSourceLabel(VoiceEngineCapabilityProfile profile)"),
                "El origen de audio debe etiquetarse desde el perfil de capacidades del motor activo.");
        assertTrue(source.contains("return LOCAL_SIMPLE_VOICE"),
                "Si la configuracion activa es Voz local simple, el sidebar no debe seguir mostrando Voz IA avanzada.");
        assertFalse(source.contains("sourceSelector.getItems().setAll(AI_VOICE, COMPUTER_AUDIO)"),
                "El combo de origen no debe reconstruirse con Voz IA avanzada fija.");
    }

    @Test
    void piperModeKeepsAdvancedControlsHidden() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        assertTrue(source.contains("profile.piperMode()"),
                "La UI de audio debe distinguir Voz local simple de motor avanzado.");
        assertTrue(source.contains("La Voz local simple usa la voz local disponible"),
                "La UI debe explicar por que tonos por muestra humana no aparecen cuando esta activo el motor simple.");
    }
}
