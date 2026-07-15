package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceEngineSettingsSourceTest {
    @Test
    void settingsExposeUsableVoiceControlsWithoutPollutingMainReader() throws Exception {
        Path settingsDialog = Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String settings = Files.readString(settingsDialog);
        assertTrue(settings.contains("VoiceEngineUsabilityPolicy.xttsHighQuality"));
        assertTrue(settings.contains("VoiceEngineUsabilityPolicy.piperLightweight"));
        assertTrue(settings.contains("Ritmo base") || settings.contains("Pausa entre oraciones"));
        assertTrue(settings.contains("VoiceSynthesisSettings.defaults()")
                || settings.contains("paragraphPauseSeconds")
                || settings.contains("Pausa entre párrafos"));
        assertTrue(settings.contains("La emoción solo aplica") || settings.contains("estilos expresivos"));
        assertTrue(settings.contains("Los comandos internos no se muestran"));
        assertFalse(settings.contains("Comando externo"));

        Path document = Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String documentSource = Files.readString(document);
        assertFalse(documentSource.contains("XTTS"));
        assertFalse(documentSource.contains("Piper"));
        assertFalse(documentSource.contains("línea de comandos"));
    }
}
