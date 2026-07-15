package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF7: SettingsDialog no crea workers manuales; delega el arranque al runner común. */
final class PresentationOrchestrationRf7SettingsWorkersSourceTest {
    @Test
    void settingsDialogUsesSharedRunnerForAllLongOperations() throws IOException {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String advancedVoice = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java");
        String piper = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java");
        String videoLocal = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java");
        String initialSetup = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java");
        String operations = dialog + advancedVoice + piper + videoLocal + initialSetup;
        String runner = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/FxBackgroundTaskRunner.java");

        assertTrue(runner.contains("public Thread start(String threadName, Runnable runnable)"));
        assertTrue(dialog.contains("private final FxBackgroundTaskRunner backgroundTaskRunner"));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"preparar-cuda-voz-ia-avanzada\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"prueba-gpu-voz-ia-avanzada\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"preparando-voz-ia-avanzada\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"descarga-modelo-voz-ia\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"importando-modelo-voz-ia\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"prueba-voz-ia-avanzada\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"reproduciendo-prueba-voz-ia\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"preparando-voz-local-simple\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"importando-voz-local-simple\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"preparando-video-local\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"importando-video-local\""));
        assertTrue(operations.contains("backgroundTaskRunner.start(\"configuracion-inicial-voz-local-simple\""));
        assertFalse(dialog.contains("Thread worker = new Thread"));
        assertFalse(dialog.contains("worker.setDaemon(true)"));
        assertFalse(dialog.contains("worker.start()"));
    }

    @Test
    void rf7IsRecordedInCurrentMarkdownLog() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");

        assertTrue(bitacora.contains("Paso RF7-01: workers de Configuración al runner común"));
        assertTrue(bitacora.contains("SettingsDialog"));
        assertTrue(bitacora.contains("FxBackgroundTaskRunner"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF7"));
        assertTrue(audit.contains("Avance RF7"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
