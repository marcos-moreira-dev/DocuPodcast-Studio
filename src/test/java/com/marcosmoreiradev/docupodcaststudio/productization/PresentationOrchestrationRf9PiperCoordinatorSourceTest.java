package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF9: Voz local simple/Piper deja de vivir como orquestación larga dentro de SettingsDialog. */
final class PresentationOrchestrationRf9PiperCoordinatorSourceTest {
    @Test
    void settingsDialogDelegatesPiperOperationsToDomainCoordinator() throws IOException {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java");

        assertTrue(dialog.contains("private final PiperSettingsOperations piperOperations"));
        assertTrue(dialog.contains("piperOperations.verify"));
        assertTrue(dialog.contains("piperOperations.confirmAndPrepare"));
        assertTrue(dialog.contains("piperOperations.runPreparation"));
        assertTrue(dialog.contains("piperOperations.selectIfReady"));
        assertTrue(dialog.contains("piperOperations.selectAndSave"));
        assertTrue(dialog.contains("piperOperations.importVoiceFolder"));
        assertFalse(dialog.contains("ImportPiperVoiceFolderUseCase"));
        assertFalse(dialog.contains("PiperVoiceImportReport"));
        assertFalse(dialog.contains("SelectPiperAsEngineUseCase"));

        assertTrue(coordinator.contains("DownloadPiperPortableRuntimeUseCase"));
        assertTrue(coordinator.contains("ImportPiperVoiceFolderUseCase"));
        assertTrue(coordinator.contains("InspectPiperSetupReadinessUseCase"));
        assertTrue(coordinator.contains("SelectPiperAsEngineUseCase"));
        assertTrue(coordinator.contains("backgroundTaskRunner.start(\"preparando-voz-local-simple\""));
        assertTrue(coordinator.contains("backgroundTaskRunner.start(\"importando-voz-local-simple\""));
    }

    @Test
    void settingsDialogStaysBelowNewRf9Limit() throws IOException {
        long lines = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java")
                .lines()
                .count();
        assertTrue(lines <= 1120, "SettingsDialog debe quedar en <= 1120 líneas tras RF9; actual=" + lines);
    }

    @Test
    void rf9IsRecordedInCurrentMarkdownLog() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");

        assertTrue(bitacora.contains("Paso RF9-01: coordinador de Voz local simple y Piper"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF9"));
        assertTrue(audit.contains("Avance RF9"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
