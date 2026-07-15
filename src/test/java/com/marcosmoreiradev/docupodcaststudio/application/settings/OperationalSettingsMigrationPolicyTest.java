package com.marcosmoreiradev.docupodcaststudio.application.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OperationalSettingsMigrationPolicyTest {
    @Test
    void repairsLocalizedAdvancedVoiceCommandIntoManagedXttsMode() {
        OperationalSettings legacy = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings(
                        "external",
                        "powershell -File \"C:/Users/MARCOS/Downloads/emer/scripts/tts/Voz IA avanzada-file-to-wav.ps1\" -ModelDir \"C:/Users/MARCOS/Downloads/emer/recursos locales IA avanzada/model.pth\"",
                        "Voz IA avanzada",
                        "es",
                        "VOC-NARRATOR",
                        180,
                        1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings("C:/Users/MARCOS/Downloads/emer/recursos locales IA avanzada/model.pth", "exports"),
                OperationalSettings.DiagnosticSettings.defaults());

        OperationalSettings repaired = OperationalSettingsMigrationPolicy.repair(legacy);

        assertEquals("xtts", repaired.tts().engineMode());
        assertEquals("", repaired.tts().commandTemplate());
        assertTrue(repaired.tts().timeoutSeconds() >= 900);
        assertEquals("models/tts/xtts", repaired.storage().modelsDirectory().replace('\\', '/'));
    }

    @Test
    void keepsNormalPiperSettingsUnchanged() {
        OperationalSettings piper = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("piper", "piper.exe -f {output}", "Voz local simple", "es", "VOC-NARRATOR", 180, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());

        OperationalSettings repaired = OperationalSettingsMigrationPolicy.repair(piper);

        assertEquals("piper", repaired.tts().engineMode());
        assertEquals("piper.exe -f {output}", repaired.tts().commandTemplate());
    }

    @Test
    void repairsDuplicatedModelPthStoragePath() {
        OperationalSettings legacy = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("xtts", "", "Voz IA avanzada", "es", "VOC-NARRATOR", 180, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings("C:/old/recursos locales IA avanzada/model.pth/model.pth", "exports"),
                OperationalSettings.DiagnosticSettings.defaults());

        OperationalSettings repaired = OperationalSettingsMigrationPolicy.repair(legacy);

        assertEquals("models/tts/xtts", repaired.storage().modelsDirectory().replace('\\', '/'));
    }
}
