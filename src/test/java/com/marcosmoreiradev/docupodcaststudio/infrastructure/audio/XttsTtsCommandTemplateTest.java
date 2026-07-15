package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XttsTtsCommandTemplateTest {
    @Test
    void derivesCoquiXttsCommandWhenEngineModeIsXttsAndNoRawCommandExists() {
        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("xtts", "", "Motor TTS local", "es", "VOC-NARRATOR", 240, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());

        String command = XttsTtsCommandTemplate.resolve(settings, Path.of("C:/DocuPodcast"))
                .orElseThrow();

        assertTrue(command.contains("xtts-file-to-wav.ps1"));
        assertTrue(command.contains("synthesize_xtts.py"));
        assertTrue(command.contains("voz-por-defecto.wav"));
        assertTrue(command.contains("-Text {textFile}"));
        assertTrue(command.contains("-Output {outputFile}"));
        assertTrue(command.contains("-Language {language}"));
    }

    @Test
    void localSimpleVoiceDoesNotLeakIntoAdvancedVoiceSpeakerPath() {
        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("xtts", "", "Voz IA avanzada", "es", "voz-local-simple", 240, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());

        String command = XttsTtsCommandTemplate.resolve(settings, Path.of("C:/DocuPodcast"))
                .orElseThrow();

        assertTrue(command.contains("voz-por-defecto.wav"));
        assertFalse(command.contains("voz-local-simple.wav"));
    }

    @Test
    void managedAdvancedVoiceIgnoresStaleRawCommandFromPreviousFolder() {
        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("coqui", "custom --text {textFile} --out {outputFile}", "Coqui", "es", "speaker-local", 240, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());

        String command = XttsTtsCommandTemplate.resolve(settings, Path.of("C:/DocuPodcastActual")).orElseThrow();

        assertTrue(command.contains("C:/DocuPodcastActual"));
        assertTrue(command.contains("xtts-file-to-wav.ps1"));
        assertFalse(command.contains("custom --text"));
    }


    @Test
    void managedCommandNormalizesSettingsThatPointToModelPth() {
        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("xtts", "", "Voz IA avanzada", "es", "VOC-NARRATOR", 240, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings("C:/Modelos/xtts/model.pth", "exports"),
                OperationalSettings.DiagnosticSettings.defaults());

        String command = XttsTtsCommandTemplate.resolve(settings, Path.of("C:/DocuPodcastActual")).orElseThrow();

        assertTrue(command.contains("C:/Modelos/xtts"));
        assertFalse(command.contains("model.pth/model.pth"));
        assertFalse(command.contains("model.pth/tts/xtts"));
    }

    @Test
    void localizedLegacyAdvancedVoiceCommandIsRebuiltAsManagedPortableCommand() {
        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("external",
                        "powershell -File C:/viejo/scripts/tts/Voz IA avanzada-file-to-wav.ps1 -Python C:/viejo/componentes locales IA avanzada-wrapper/.venv/Scripts/python.exe -ModelDir \"C:/viejo/recursos locales IA avanzada/model.pth\"",
                        "Voz IA avanzada", "es", "VOC-NARRATOR", 240, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings("C:/viejo/recursos locales IA avanzada/model.pth", "exports"),
                OperationalSettings.DiagnosticSettings.defaults());

        String command = XttsTtsCommandTemplate.resolve(settings, Path.of("C:/DocuPodcastActual")).orElseThrow();

        assertTrue(command.contains("C:/DocuPodcastActual"));
        assertTrue(command.contains("scripts/tts/xtts-file-to-wav.ps1"));
        assertTrue(command.contains("tools/xtts-wrapper/.venv/Scripts/python.exe"));
        assertTrue(command.contains("tools/xtts-wrapper/synthesize_xtts.py"));
        assertTrue(command.contains("models/tts/xtts"));
        assertFalse(command.contains("Voz IA avanzada-file-to-wav.ps1"));
        assertFalse(command.contains("componentes locales IA avanzada-wrapper"));
        assertFalse(command.contains("recursos locales IA avanzada"));
        assertFalse(command.contains("C:/viejo"));
        assertFalse(command.contains("model.pth/model.pth"));
    }

}
