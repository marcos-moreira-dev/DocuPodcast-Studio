package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF8: Voz IA avanzada/CUDA deja de vivir como orquestación larga dentro de SettingsDialog. */
final class PresentationOrchestrationRf8AdvancedVoiceCoordinatorSourceTest {
    @Test
    void settingsDialogDelegatesAdvancedVoiceOperationsToDomainCoordinator() throws IOException {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java");

        assertTrue(dialog.contains("private final AdvancedVoiceSettingsOperations advancedVoiceOperations"));
        assertTrue(dialog.contains("advancedVoiceOperations.prepareCuda"));
        assertTrue(dialog.contains("advancedVoiceOperations.runCudaSmoke"));
        assertTrue(dialog.contains("advancedVoiceOperations.confirmAndPrepare"));
        assertTrue(dialog.contains("advancedVoiceOperations.confirmAndDownloadModel"));
        assertTrue(dialog.contains("advancedVoiceOperations.importModelFolder"));
        assertTrue(dialog.contains("advancedVoiceOperations.runReadinessSmoke"));
        assertTrue(dialog.contains("advancedVoiceOperations.playAndConfirmSmoke"));
        assertTrue(dialog.contains("advancedVoiceOperations.selectIfReady"));
        assertFalse(dialog.contains("PrepareXttsPortableRuntimeUseCase"));
        assertFalse(dialog.contains("DownloadXttsOfficialModelUseCase"));
        assertFalse(dialog.contains("ImportXttsModelFolderUseCase"));

        assertTrue(coordinator.contains("PrepareXttsPortableRuntimeUseCase"));
        assertTrue(coordinator.contains("DownloadXttsOfficialModelUseCase"));
        assertTrue(coordinator.contains("ImportXttsModelFolderUseCase"));
        assertTrue(coordinator.contains("RunXttsReadinessSmokeUseCase"));
        assertTrue(coordinator.contains("ConfirmXttsSmokePlaybackUseCase"));
        assertTrue(coordinator.contains("SelectXttsAsEngineUseCase"));
        assertTrue(coordinator.contains("OperationalSettingsMigrationPolicy.repair"));
        assertTrue(coordinator.contains("backgroundTaskRunner.start(\"preparando-voz-ia-avanzada\""));
    }

    @Test
    void settingsDialogStaysBelowNewRf8Limit() throws IOException {
        long lines = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java")
                .lines()
                .count();
        assertTrue(lines <= 1300, "SettingsDialog debe quedar en <= 1300 líneas tras RF8; actual=" + lines);
    }

    @Test
    void rf8IsRecordedInCurrentMarkdownLog() throws IOException {
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String audit = read("DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md");

        assertTrue(bitacora.contains("Paso RF8-01: coordinador de Voz IA avanzada y CUDA"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF8"));
        assertTrue(audit.contains("Avance RF8"));
    }

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
