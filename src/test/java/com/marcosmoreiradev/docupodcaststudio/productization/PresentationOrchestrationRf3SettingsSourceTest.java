package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF3: Configuración keeps use-case orchestration while shared progress UI lives outside SettingsDialog. */
final class PresentationOrchestrationRf3SettingsSourceTest {
    @Test
    void settingsOperationProgressLivesInDedicatedCoordinator() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String advancedVoice = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java");
        String piper = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java");
        String videoLocal = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java");
        String initialSetup = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java");
        String settingsSurface = dialog + advancedVoice + piper + videoLocal + initialSetup;
        String progress = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsOperationProgressCoordinator.java");

        assertTrue(dialog.contains("SettingsOperationProgressCoordinator"));
        assertTrue(settingsSurface.contains("operationProgress.show"));
        assertFalse(dialog.contains("private OperationProgress showOperationProgress"));
        assertFalse(dialog.contains("private static final class OperationProgress"));
        assertFalse(dialog.contains("ProgressBar progress = new ProgressBar"));
        assertFalse(dialog.contains("settings-operation-heartbeat"));

        assertTrue(progress.contains("final class SettingsOperationProgressCoordinator"));
        assertTrue(progress.contains("final class OperationProgress"));
        assertTrue(progress.contains("ProgressBar"));
        assertTrue(progress.contains("settings-operation-heartbeat"));
        assertTrue(progress.contains("Última actualización"));
        assertTrue(progress.contains("Operación activa"));
    }

    @Test
    void settingsDialogStaysUnderRf3LineBudget() throws Exception {
        long lines = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java")
                .lines()
                .count();
        assertTrue(lines <= 1800, "SettingsDialog debe quedar en <= 1800 líneas; actual=" + lines);
    }

    @Test
    void rf3IsRecordedInCurrentMarkdownLog() throws Exception {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        assertTrue(bitacora.contains("Paso RF3-01"));
        assertTrue(bitacora.contains("SettingsOperationProgressCoordinator"));
        assertTrue(bitacora.contains("Pendiente siguiente"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF3"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
