package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RunXttsReadinessSmokeUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesSmokeWavManifestOnlyAfterStrictReadinessPasses() throws Exception {
        createXttsRuntime(tempDir);
        VoiceTestSynthesisGateway fakeGateway = request -> {
            Files.write(request.outputFile(), wavBytes());
            return VoiceTestSynthesisResult.generated(Files.size(request.outputFile()), "WAV real de test generado.",
                    "DOCUPODCAST_XTTS: device_requested=gpu-nvidia-0\n"
                            + "DOCUPODCAST_XTTS: device_backend=cuda device_name=NVIDIA GeForce GTX 1650\n"
                            + "DOCUPODCAST_XTTS: device=cuda:0\n"
                            + "DOCUPODCAST_XTTS: asignando_device=cuda:0\n");
        };
        XttsSmokeTestReport report = new RunXttsReadinessSmokeUseCase(
                new InspectXttsSetupReadinessUseCase(), fakeGateway)
                .run(OperationalSettings.defaults(), tempDir);

        assertTrue(report.generatedWavProof());
        assertFalse(report.playbackConfirmed());
        assertTrue(Files.isRegularFile(report.audioFile()));
        assertTrue(Files.readString(report.manifestFile()).contains("docupodcast-xtts-readiness-smoke-v1"));
        assertTrue(Files.readString(report.manifestFile()).contains("\"playbackConfirmed\": false"));
        assertTrue(report.userMessage().contains("cuda:0"));
        assertTrue(Files.readString(report.manifestFile()).contains("\"runtimeEvidence\""));
        assertTrue(Files.readString(report.manifestFile()).contains("NVIDIA GeForce GTX 1650"));
    }

    @Test
    void blocksSmokeWhenRuntimeIsIncomplete() throws Exception {
        XttsSmokeTestReport report = new RunXttsReadinessSmokeUseCase()
                .run(OperationalSettings.defaults(), tempDir);

        assertFalse(report.generatedWavProof());
        assertTrue(report.userMessage().contains("no está seleccionable") || !report.issues().isEmpty());
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
        Files.writeString(root.resolve("models/tts/xtts/SHA256SUMS.txt"), "mock checksum");
        Files.write(root.resolve("models/tts/xtts/speakers/voz-por-defecto.wav"), wavBytes());
    }

    private static byte[] wavBytes() {
        byte[] data = new byte[128];
        data[0] = 'R';
        data[1] = 'I';
        data[2] = 'F';
        data[3] = 'F';
        data[8] = 'W';
        data[9] = 'A';
        data[10] = 'V';
        data[11] = 'E';
        return data;
    }
}
