package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiperTtsCommandTemplateTest {
    @Test
    void derivesPiperCommandWhenEngineModeIsPiperAndCommandIsEmpty() {
        OperationalSettings settings = settings("piper", "", "es_ES-test-medium");

        String command = PiperTtsCommandTemplate.resolve(settings, Path.of(".")).orElseThrow();

        assertTrue(command.contains("piper-file-to-wav.ps1"));
        assertTrue(command.contains("tools/piper/piper.exe"));
        assertTrue(command.contains("models/tts/piper/voices/es_ES-test-medium.onnx"));
        assertTrue(command.contains("{textFile}"));
        assertTrue(command.contains("{outputFile}"));
        assertTrue(command.contains("-ComputePolicy {computePolicy}"));
        assertTrue(command.contains("-Device {computeDevice}"));
        assertTrue(command.contains("-GpuIndex {gpuIndex}"));
    }

    @Test
    void managedSimpleVoiceIgnoresStaleRawCommandFromPreviousFolder() {
        OperationalSettings settings = settings("piper", "custom --text {textFile} --out {outputFile}", "es_ES-test-medium");

        String command = PiperTtsCommandTemplate.resolve(settings, Path.of("C:/DocuPodcastActual")).orElseThrow();

        assertTrue(command.contains("C:/DocuPodcastActual"));
        assertTrue(command.contains("piper-file-to-wav.ps1"));
        assertTrue(command.contains("es_ES-test-medium.onnx"));
    }

    @Test
    void advancedPresetVoiceDoesNotLeakIntoPiperCommand() {
        OperationalSettings settings = settings("piper", "", "VOC-PRESET-MUJER-ADULTA-CALIDA-NARRATIVA");

        String command = PiperTtsCommandTemplate.resolve(settings, Path.of("C:/DocuPodcastActual")).orElseThrow();

        assertTrue(command.contains("models/tts/piper/voices/es_ES-default-medium.onnx"));
        assertFalse(command.contains("VOC-PRESET"));
    }

    @Test
    void readinessRequiresWrapperExecutableAndVoiceModel() throws Exception {
        Path root = Files.createTempDirectory("docupodcast-piper-ready");
        Files.createDirectories(root.resolve("scripts/tts"));
        Files.createDirectories(root.resolve("tools/piper"));
        Files.createDirectories(root.resolve("models/tts/piper/voices"));
        Files.writeString(root.resolve("scripts/tts/piper-file-to-wav.ps1"), "# wrapper");
        Files.writeString(root.resolve("tools/piper/piper.exe"), "fake");
        Files.writeString(root.resolve("models/tts/piper/voices/es_ES-test-medium.onnx"), "voice");

        assertTrue(PiperTtsCommandTemplate.looksReady(settings("piper", "", "es_ES-test-medium"), root));
    }

    private static OperationalSettings settings(String mode, String command, String voice) {
        return new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings(mode, command, "Piper local", "es", voice, 180, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());
    }
}
