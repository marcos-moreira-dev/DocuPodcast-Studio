package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectPiperSetupReadinessUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsReadyIntermediatePiperSetupWithOnnxAndMetadata() throws Exception {
        writeReadyPiperLayout();

        PiperSetupReadinessReport report = new InspectPiperSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertTrue(report.ready());
        assertTrue(report.userMessage().contains("liviano"));
        assertTrue(report.voiceModel().getFileName().toString().endsWith(".onnx"));
    }

    @Test
    void advancedVoicePresetSelectionStillUsesManagedPiperVoice() throws Exception {
        writeReadyPiperLayout();
        OperationalSettings settings = settingsWithVoiceAndStorage("VOC-PRESET-MUJER-ADULTA-CALIDA-NARRATIVA", "models");

        PiperSetupReadinessReport report = new InspectPiperSetupReadinessUseCase().inspect(settings, tempDir);

        assertTrue(report.ready());
        assertTrue(report.voiceModel().endsWith(Path.of("models/tts/piper/voices/es_ES-default-medium.onnx")));
        assertFalse(report.voiceModel().toString().contains("VOC-PRESET"));
    }

    @Test
    void legacyLocalizedSimpleVoiceStorageFallsBackToEmbeddedModels() throws Exception {
        writeReadyPiperLayout();
        OperationalSettings settings = settingsWithVoiceAndStorage(
                "VOC-PRESET-MUJER-ADULTA-CALIDA-NARRATIVA",
                "recursos locales local simple");

        PiperSetupReadinessReport report = new InspectPiperSetupReadinessUseCase().inspect(settings, tempDir);

        assertTrue(report.ready());
        assertEquals(tempDir.resolve("models/tts/piper/voices").normalize(), report.voiceDirectory());
    }

    @Test
    void reportsMissingMetadataBecausePiperUsesModelNotHumanSample() throws Exception {
        Files.createDirectories(tempDir.resolve("scripts/tts"));
        Files.writeString(tempDir.resolve("scripts/tts/piper-file-to-wav.ps1"), "wrapper");
        Files.createDirectories(tempDir.resolve("tools/piper"));
        Files.writeString(tempDir.resolve("tools/piper/piper.exe"), "exe");
        Path voices = tempDir.resolve("models/tts/piper/voices");
        Files.createDirectories(voices);
        Files.writeString(voices.resolve("es_ES-default-medium.onnx"), "voice");

        PiperSetupReadinessReport report = new InspectPiperSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertFalse(report.ready());
        assertTrue(report.missingRequirements().stream().anyMatch(item -> item.contains(".onnx.json")));
    }

    private void writeReadyPiperLayout() throws Exception {
        Files.createDirectories(tempDir.resolve("scripts/tts"));
        Files.writeString(tempDir.resolve("scripts/tts/piper-file-to-wav.ps1"), "wrapper");
        Files.createDirectories(tempDir.resolve("tools/piper"));
        Files.writeString(tempDir.resolve("tools/piper/piper.exe"), "exe");
        Path voices = tempDir.resolve("models/tts/piper/voices");
        Files.createDirectories(voices);
        Files.writeString(voices.resolve("es_ES-default-medium.onnx"), "voice");
        Files.writeString(voices.resolve("es_ES-default-medium.onnx.json"), "{}");
        Files.writeString(voices.resolve("SHA256SUMS.txt"), "checksum");
    }

    private static OperationalSettings settingsWithVoiceAndStorage(String voice, String modelsDirectory) {
        OperationalSettings defaults = OperationalSettings.defaults();
        return new OperationalSettings(
                defaults.readingDocument(),
                defaults.playbackBuffer(),
                new OperationalSettings.TtsEngineSettings(
                        "piper",
                        "",
                        "Piper local",
                        "es",
                        voice,
                        180,
                        1),
                defaults.video(),
                defaults.compute(),
                new OperationalSettings.StorageSettings(modelsDirectory, "exports"),
                defaults.diagnostics());
    }
}
