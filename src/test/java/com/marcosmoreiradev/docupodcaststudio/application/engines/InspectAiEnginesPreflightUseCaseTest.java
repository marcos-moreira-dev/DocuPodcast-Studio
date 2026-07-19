package com.marcosmoreiradev.docupodcaststudio.application.engines;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectAiEnginesPreflightUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void coquiIsMandatoryEvenWhenPiperCouldBeFallback() throws Exception {
        Path piper = tempDir.resolve("models/tts/piper/voices");
        Files.createDirectories(piper);
        Files.writeString(piper.resolve("es-test.onnx"), "fake voice");
        Files.writeString(piper.resolve("es-test.onnx.json"), "{}");

        OperationalSettings settings = new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("piper", "piper --model {voice} --output_file {outputFile}", "Piper", "es", "VOC-NARRATOR", 180, 1),
                OperationalSettings.VideoRenderSettings.defaults(),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings(tempDir.resolve("models").toString(), "exports"),
                OperationalSettings.DiagnosticSettings.defaults());

        AiEnginePreflightReport report = new InspectAiEnginesPreflightUseCase().inspect(settings, tempDir);

        assertTrue(report.items().stream().anyMatch(item -> item.engineId().equals("tts-xtts-coqui") && item.mandatoryForTargetProduct()));
        assertTrue(report.piperReady(), "Piper puede quedar listo para demo liviana");
        assertFalse(report.coquiMandatoryButReadyOrWarnOnly(), "Coqui sigue pendiente aunque Piper funcione");
        assertTrue(report.summary().contains("Voz=sí"));
        assertFalse(report.items().stream().anyMatch(item -> item.engineId().contains("whisper")), "Whisper no pertenece al preflight visible de producto.");
    }

    @Test
    void advancedVoiceWithCompleteRuntimeIsUsableBeforeOptionalWavProof() throws Exception {
        OperationalSettings settings = fullXttsSettings(false);

        AiEnginePreflightReport report = new InspectAiEnginesPreflightUseCase().inspect(settings, tempDir);

        AiEnginePreflightItem xtts = report.items().stream()
                .filter(item -> item.engineId().equals("tts-xtts-coqui"))
                .findFirst()
                .orElseThrow();
        assertEquals(AiEngineReadinessStatus.READY_WITH_WARNINGS, xtts.status());
        assertTrue(xtts.usable());
        assertTrue(report.coquiMandatoryButReadyOrWarnOnly());
        assertTrue(xtts.userMessage().contains("puede generar audio"));
    }

    @Test
    void fullProductDemoRequiresVoiceWavProofAndFfmpeg() throws Exception {
        OperationalSettings settings = fullXttsSettings(true);
        writeSmoke(tempDir, true);

        AiEnginePreflightReport report = new InspectAiEnginesPreflightUseCase().inspect(settings, tempDir);

        assertTrue(report.coquiMandatoryButReadyOrWarnOnly());
        assertTrue(report.ffmpegReady());
        assertTrue(report.fullAiDemoReady());
    }

    @Test
    void reportsOcrTesseractReadinessSeparately() throws Exception {
        Path tesseract = tempDir.resolve("tools/tesseract/bin/tesseract.exe");
        Files.createDirectories(tesseract.getParent());
        Files.writeString(tesseract, "fake");
        Path tessdata = tempDir.resolve("tools/tesseract/tessdata");
        Files.createDirectories(tessdata);
        Files.writeString(tessdata.resolve("spa.traineddata"), "spa");
        Files.writeString(tessdata.resolve("eng.traineddata"), "eng");

        AiEnginePreflightReport report = new InspectAiEnginesPreflightUseCase().inspect(OperationalSettings.defaults(), tempDir);

        assertTrue(report.ocrReady());
        assertTrue(report.summary().contains("OCR=s"));
        assertTrue(report.items().stream().anyMatch(item -> item.engineId().equals("ocr-tesseract")
                && item.purpose() == AiEnginePurpose.OCR_TEXT_EXTRACTION));
    }

    private OperationalSettings fullXttsSettings(boolean ffmpeg) throws Exception {
        Path models = tempDir.resolve("models");
        Path xtts = models.resolve("tts/xtts");
        Files.createDirectories(xtts);
        Files.writeString(xtts.resolve("config.json"), "{}");
        Files.writeString(xtts.resolve("model.pth"), "fake");
        Files.writeString(xtts.resolve("vocab.json"), "{}");
        Files.writeString(xtts.resolve("speakers_xtts.pth"), "speakers");
        Files.writeString(xtts.resolve("dvae.pth"), "dvae");
        Files.writeString(xtts.resolve("mel_stats.pth"), "stats");
        Files.writeString(xtts.resolve("sha256sums.txt"), "fake");
        Path speakers = xtts.resolve("speakers");
        Files.createDirectories(speakers);
        Files.write(speakers.resolve("voz-por-defecto.wav"), wavBytes());
        Files.createDirectories(tempDir.resolve("tools/xtts-wrapper/.venv/Scripts"));
        Files.createDirectories(tempDir.resolve("scripts/tts"));
        Files.writeString(tempDir.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe"), "python");
        Files.writeString(tempDir.resolve("tools/xtts-wrapper/synthesize_xtts.py"), "wrapper");
        Files.writeString(tempDir.resolve("scripts/tts/xtts-file-to-wav.ps1"), "script");
        Path ffmpegPath = tempDir.resolve("ffmpeg.exe");
        if (ffmpeg) {
            Files.writeString(ffmpegPath, "fake ffmpeg");
        }

        return new OperationalSettings(
                OperationalSettings.ReadingDocumentSettings.defaults(),
                OperationalSettings.PlaybackBufferSettings.defaults(),
                new OperationalSettings.TtsEngineSettings("xtts", "xtts-wrapper --text {textFile} --out {outputFile}", "Coqui XTTS", "es", "VOC-NARRATOR", 180, 1),
                new OperationalSettings.VideoRenderSettings(ffmpegPath.toString(), "2K", false),
                OperationalSettings.ComputeSettings.defaults(),
                new OperationalSettings.StorageSettings(models.toString(), "exports"),
                OperationalSettings.DiagnosticSettings.defaults());
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
