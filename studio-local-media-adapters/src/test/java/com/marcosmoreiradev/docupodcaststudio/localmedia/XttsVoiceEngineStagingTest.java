package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XttsVoiceEngineStagingTest {
    @Test
    void recognizesBothManagedBatchAndPersistentWorkerStaging() {
        assertTrue(XttsVoiceEngine.managedStagingDirectory(
                Path.of(".voice-batch-123")));
        assertTrue(XttsVoiceEngine.managedStagingDirectory(
                Path.of(".voice-worker-456")));
        assertFalse(XttsVoiceEngine.managedStagingDirectory(
                Path.of("audio")));
    }

    @Test
    void preservesFailedUnitTextAndWorkerLogOutsideStaging(
            @TempDir Path temporary) throws Exception {
        Path audio = temporary.resolve("audio");
        Path workerLog = temporary.resolve(".voice-worker-1")
                .resolve("xtts-worker.log");
        Files.createDirectories(workerLog.getParent());
        Files.writeString(workerLog, "fallo controlado",
                StandardCharsets.UTF_8);
        var unit = new com.marcosmoreiradev.docupodcaststudio.media.api.VoiceSynthesisUnit(
                "PDFSEG-PRUEBA-U007", "La expresión a i ≥ 0",
                "voice", "neutral", null, audio.resolve("unit.wav"), Map.of());

        Path diagnostic = XttsVoiceEngine.preserveWorkerFailureFiles(
                audio, workerLog, unit);

        assertTrue(Files.isRegularFile(diagnostic.resolve("texto.txt")));
        assertTrue(Files.isRegularFile(diagnostic.resolve("motor.log")));
        assertEquals("La expresión a i ≥ 0",
                Files.readString(diagnostic.resolve("texto.txt"),
                        StandardCharsets.UTF_8));
    }

    @Test
    void forcesUtf8ForGuiLaunchedPythonWorkerOnWindows() {
        ProcessBuilder processBuilder = new ProcessBuilder("python");

        XttsBatchWorker.configureUtf8Environment(processBuilder);

        assertEquals("1", processBuilder.environment().get("PYTHONUTF8"));
        assertEquals("utf-8",
                processBuilder.environment().get("PYTHONIOENCODING"));
    }
}
