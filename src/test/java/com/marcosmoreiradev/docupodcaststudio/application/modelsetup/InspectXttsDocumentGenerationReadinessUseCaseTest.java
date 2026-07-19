package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectXttsDocumentGenerationReadinessUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void incompleteRuntimeStillBlocksDocumentGeneration() {
        XttsDocumentGenerationReadinessReport report = new InspectXttsDocumentGenerationReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertFalse(report.canGenerateDocumentAudio());
        assertFalse(report.blockingReasons().isEmpty());
    }

    @Test
    void completeRuntimeCanGenerateDocumentsBeforeOptionalWavProof() throws Exception {
        createXttsRuntime(tempDir);

        XttsDocumentGenerationReadinessReport report = new InspectXttsDocumentGenerationReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertTrue(report.canGenerateDocumentAudio());
        assertTrue(report.blockingReasons().isEmpty());
        assertTrue(report.warnings().stream().anyMatch(warning -> warning.contains("prueba WAV corta")));
        assertTrue(report.userMessage().contains("puede generar audio"));
    }

    @Test
    void generatedWavProofAllowsDocumentAudioButStillRequestsPlaybackConfirmation() throws Exception {
        createXttsRuntime(tempDir);
        writeSmoke(tempDir, false);

        XttsDocumentGenerationReadinessReport report = new InspectXttsDocumentGenerationReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertTrue(report.canGenerateDocumentAudio());
        assertFalse(report.playbackConfirmed());
        assertTrue(report.needsUserPlaybackConfirmation());
        assertTrue(report.warnings().stream().anyMatch(warning -> warning.contains("confirmar reproducción")));
    }

    @Test
    void fullyVerifiedSmokeClosesTheGate() throws Exception {
        createXttsRuntime(tempDir);
        writeSmoke(tempDir, true);

        XttsDocumentGenerationReadinessReport report = new InspectXttsDocumentGenerationReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertTrue(report.canGenerateDocumentAudio());
        assertTrue(report.playbackConfirmed());
        assertFalse(report.needsUserPlaybackConfirmation());
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

    private static void writeSmoke(Path root, boolean playbackConfirmed) throws Exception {
        Path dir = root.resolve("runtime/tts/xtts-smoke");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("xtts-readiness-smoke.txt"), "prueba");
        Files.write(dir.resolve("xtts-readiness-smoke.wav"), wavBytes());
        Files.writeString(dir.resolve("xtts-readiness-smoke.json"), "{\n"
                + "  \"generatedAt\": \"2026-06-06T00:00:00Z\",\n"
                + "  \"generated\": true,\n"
                + "  \"playbackConfirmed\": " + playbackConfirmed + "\n"
                + "}\n");
    }

    private static byte[] wavBytes() {
        byte[] bytes = new byte[96];
        bytes[0] = 'R';
        bytes[1] = 'I';
        bytes[2] = 'F';
        bytes[3] = 'F';
        return bytes;
    }
}
