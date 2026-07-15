package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectXttsSetupReadinessUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void reportsReadyWhenPortablePythonWrapperModelAndNeutralVoiceExist() throws Exception {
        createXttsRuntime(tempDir);
        Files.writeString(tempDir.resolve("models/tts/xtts/SHA256SUMS.txt"), "mock checksum");

        XttsSetupReadinessReport report = new InspectXttsSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertTrue(report.ready());
        assertTrue(report.canBeSelectedAsEngine());
        assertTrue(report.missingRequirements().isEmpty());
        assertTrue(report.userMessage().contains("list"));
    }

    @Test
    void reportsMissingPortablePythonWithoutUsingGlobalPython() throws Exception {
        Files.createDirectories(tempDir.resolve("tools/xtts-wrapper"));
        Files.createDirectories(tempDir.resolve("scripts/tts"));
        Files.createDirectories(tempDir.resolve("models/tts/xtts/speakers"));
        Files.writeString(tempDir.resolve("scripts/tts/setup-xtts-portable-python.ps1"), "setup");
        Files.writeString(tempDir.resolve("tools/xtts-wrapper/synthesize_xtts.py"), "wrapper");
        Files.writeString(tempDir.resolve("models/tts/xtts/config.json"), "{}");
        Files.writeString(tempDir.resolve("models/tts/xtts/model.pth"), "weights");
        Files.writeString(tempDir.resolve("models/tts/xtts/vocab.json"), "{}");
        Files.writeString(tempDir.resolve("models/tts/xtts/speakers_xtts.pth"), "speakers");
        Files.writeString(tempDir.resolve("models/tts/xtts/dvae.pth"), "dvae");
        Files.writeString(tempDir.resolve("models/tts/xtts/mel_stats.pth"), "stats");
        Files.writeString(tempDir.resolve("models/tts/xtts/speakers/voz-por-defecto.wav"), "wav");

        XttsSetupReadinessReport report = new InspectXttsSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertFalse(report.ready());
        assertTrue(report.missingRequirements().stream().anyMatch(item -> item.contains("Python local portable")));
        assertTrue(report.pythonExecutable().toString().replace('\\', '/').contains("tools/xtts-wrapper/.venv/Scripts/python.exe"));
    }

    @Test
    void ignoresLocalSimpleVoiceWhenInspectingAdvancedVoiceSetup() throws Exception {
        createXttsRuntime(tempDir);
        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("piper", "", "Voz local simple", "es", "voz-local-simple", 180, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                OperationalSettings.StorageSettings.defaults(),
                OperationalSettings.DiagnosticSettings.defaults());

        XttsSetupReadinessReport report = new InspectXttsSetupReadinessUseCase()
                .inspect(settings, tempDir);

        assertTrue(report.ready());
        assertTrue(report.speakerWav().toString().replace('\\', '/').endsWith("models/tts/xtts/speakers/voz-por-defecto.wav"));
        assertFalse(report.missingRequirements().stream().anyMatch(item -> item.contains("voz-local-simple")));
    }

    private static void createXttsRuntime(Path root) throws Exception {
        Files.createDirectories(root.resolve("tools/xtts-wrapper/.venv/Scripts"));
        Files.createDirectories(root.resolve("scripts/tts"));
        Files.createDirectories(root.resolve("models/tts/xtts/speakers"));
        Files.writeString(root.resolve("scripts/tts/setup-xtts-portable-python.ps1"), "setup");
        Files.writeString(root.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe"), "python");
        Files.writeString(root.resolve("tools/xtts-wrapper/synthesize_xtts.py"), "wrapper");
        Files.writeString(root.resolve("models/tts/xtts/config.json"), "{}");
        Files.writeString(root.resolve("models/tts/xtts/model.pth"), "weights");
        Files.writeString(root.resolve("models/tts/xtts/vocab.json"), "{}");
        Files.writeString(root.resolve("models/tts/xtts/speakers_xtts.pth"), "speakers");
        Files.writeString(root.resolve("models/tts/xtts/dvae.pth"), "dvae");
        Files.writeString(root.resolve("models/tts/xtts/mel_stats.pth"), "stats");
        Files.writeString(root.resolve("models/tts/xtts/speakers/voz-por-defecto.wav"), "wav");
    }
}
